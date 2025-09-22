package com.example.pp.service;




import com.example.pp.config.SeoulMetroClient;
import com.example.pp.config.TourApiV2Client;
import com.example.pp.dto.LineStationsResponse;
import com.example.pp.dto.TourPoiResponse;
import com.example.pp.entity.staion_info;
import com.example.pp.repository.SubwayStationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class List1AllNearService {

    private static final Logger log = LoggerFactory.getLogger(List1AllNearService.class);
    private static final double NEAR_RADIUS_M = 100.0;

    private final SubwayStationRepository stationRepo;
    private final SeoulMetroClient seoul;  // 노선별 정차역 API 사용
    private final TourApiV2Client tour;    // 위치기반 관광지 API 사용

    // 리스트1 전체 원본을 저장하는 간단한 메모리 저장소 (contentid 키)
    private final Map<String, TourPoiResponse.Item> store = new LinkedHashMap<>();

    private record Seed(double lon, double lat) {}
    private record Coord(double lon, double lat) {}

    // 빌드: List1을 “TourAPI Item 원본”으로 반환 + 저장소에 보관
    public Mono<List<TourPoiResponse.Item>> build(double lat, double lon, int tourRadiusMeters, int pageSize) {
        // 1) 주변 100m 내 모든 역(거리순)
        List<staion_info> nears = stationRepo.findAllWithinRadiusOrderByDistanceAsc(lat, lon);
        if (nears.isEmpty()) {
            log.warn("[주변 역 없음] lat={}, lon={}", lat, lon);
            return Mono.just(List.of());
        }

        // 2) 각 역의 노선 정차역 전체 조회 → 기준 역명 인덱스 → +1, +2 역명
        return Flux.fromIterable(nears)
                .flatMap(st -> seoul.fetchLineStationsMax(st.getLineName())
                        .map(resp -> toNextTwoNames(resp, st.getStationName())))
                .flatMap(Flux::fromIterable)
                .map(this::normalizeName)
                .distinct()
                // 3) 두(여러) 역명 → 좌표 해석(이름 우선)
                .map(this::resolveByNameFirst)
                .filter(c -> !(c.lon == 0 && c.lat == 0))
                // 좌표 중복 제거(경도+위도 키)
                .distinct(c -> c.lon + "," + c.lat)
                .map(c -> new Seed(c.lon, c.lat))
                .collectList()
                .flatMap(seeds -> {
                    if (seeds.isEmpty()) {
                        log.warn("[시드 좌표 없음] TourAPI 호출 생략");
                        return Mono.just(List.<TourPoiResponse.Item>of());
                    }
                    // 4) 좌표별 TourAPI 위치기반 호출 → contentid로 중복 제거 → dist 오름차순
                    return Flux.fromIterable(seeds)
                            .flatMap(seed ->
                                    tour.locationBasedList2(seed.lon, seed.lat, tourRadiusMeters, 1, pageSize, "C")
                                            .map(r -> Optional.ofNullable(r.response())
                                                    .map(TourPoiResponse.Resp::body)
                                                    .map(TourPoiResponse.Body::items)
                                                    .map(TourPoiResponse.Items::item)
                                                    .orElseGet(List::of))
                                            .doOnNext(list -> log.info("[TourAPI 위치기반] seed=({}, {}) 수신={}",
                                                    seed.lat, seed.lon, list.size()))
                                            .flatMapIterable(i -> i)
                            )
                            .collect(Collectors.toMap(TourPoiResponse.Item::contentid, i -> i, (a, b) -> a))
                            .map(map -> {
                                List<TourPoiResponse.Item> list = new ArrayList<>(map.values());
                                list.sort(Comparator.comparing(List1AllNearService::safeDist)); // dist 오름차순
                                // 5) 저장소에 보관(필요 시 후처리로 일부만 남기고 제거)
                                list.forEach(it -> store.put(it.contentid(), it));
                                return list;
                            });
                });
    }

    // 저장된 리스트1 전체 가져오기(필요 시 사용)
    public List<TourPoiResponse.Item> getSavedList1() {
        return new ArrayList<>(store.values());
    }

    // contentid로 선택 제거(“사용하지 않을 것” 정리용)
    public void removeFromSavedList1(String contentId) {
        store.remove(contentId);
    }

    // 정차역 응답에서 기준 역명을 찾아 다음 2개 역명을 반환
    private List<String> toNextTwoNames(LineStationsResponse resp, String baseStationName) {
        var stops = Optional.ofNullable(resp.service())
                .map(LineStationsResponse.ServiceBlock::rows)
                .orElseGet(List::of);
        if (stops.isEmpty()) return List.of();

        String target = normalizeName(baseStationName);
        int idx = -1;
        for (int i = 0; i < stops.size(); i++) {
            String nameKo = normalizeName(stops.get(i).stationNameKo());
            if (target.equalsIgnoreCase(nameKo)) { idx = i; break; }
        }
        if (idx < 0) return List.of();

        int i1 = Math.min(idx + 1, stops.size() - 1);
        int i2 = Math.min(idx + 2, stops.size() - 1);
        return List.of(stops.get(i1).stationNameKo(), stops.get(i2).stationNameKo());
    }

    // 역명 → 좌표(이름 우선, 정규화 보조)
    private Coord resolveByNameFirst(String stationName) {
        String normalized = normalizeName(stationName);
        return stationRepo.findOneByStationName(stationName)
                .or(() -> stationRepo.findFirstByStationNameIgnoreCase(stationName))
                .or(() -> stationRepo.findOneByStationNameNormalized(normalized))
                .map(s -> new Coord(s.getLon(), s.getLat()))
                .orElse(new Coord(0, 0));
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.replace("(", " ").replace(")", " ")
                .replaceAll("\\s+", " ").trim();
    }

    // TourAPI dist는 보통 문자열이므로 안전 파싱 후 정렬용 Double 반환
    private static Double safeDist(TourPoiResponse.Item i) {
        String d = i.dist();
        if (d == null) return Double.POSITIVE_INFINITY;
        try { return Double.parseDouble(d); }
        catch (Exception e) { return Double.POSITIVE_INFINITY; }
    }
}
