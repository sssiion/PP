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
import java.util.function.Function;
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
    private record LineCtx(List<LineStationsResponse.Row> stops, Map<String,Integer> nameToIdx) {}


    // 빌드: List1을 “TourAPI Item 원본”으로 반환 + 저장소에 보관
    public Mono<List<TourPoiResponse.Item>> build(double lat, double lon, int tourRadiusMeters, int pageSize) {
        // 1) 주변 100m 내 모든 역(거리순)
        List<staion_info> nears = stationRepo.findAllWithinRadiusOrderByDistanceAsc(lat, lon);
        if (nears.isEmpty()) {
            log.warn("[주변 역 없음] lat={}, lon={}", lat, lon);
            return Mono.just(List.of());
        }
        // 2) 라인명/역명 조합 정렬(Set: 라인별 1회만 요청)
        Set<String> lines = nears.stream()
                .map(staion_info::getLineName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3) 라인 전체 정차역을 1회씩만 비동기 조회 & key로 맵화
        return Flux.fromIterable(lines)
                .flatMap(line -> seoul.fetchLineStationsMax(line)
                        .map(stops -> {
                            Map<String, Integer> nameToIdx = new HashMap<>();
                            for (int i = 0; i < stops.size(); i++) {
                                nameToIdx.putIfAbsent(normalizeName(stops.get(i).stationNameKo()), i);
                            }
                            return Map.entry(line, new LineCtx(stops, nameToIdx));
                        })
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(lineCtxMap -> {
                    // (정규화된 역명 기준) Set<String>으로 seed 역명 중복 필터
                    Set<String> nextStationsNormNames = new LinkedHashSet<>();
                    Map<String, String> normToOrigName = new HashMap<>(); // 정규화 → 원본(정확한) 이름 치환용
                    // 4) 한 역(=노선,역명)마다 다음 2개 역명 추출(중복 역 필터)
                    //LinkedHashSet<String> nextStations = new LinkedHashSet<>();
                    for (staion_info st : nears) {
                        String line = st.getLineName();
                        String baseName = normalizeName(st.getStationName());
                        LineCtx ctx = lineCtxMap.get(line);
                        if (ctx == null || ctx.stops().isEmpty()) continue;
                        Integer idx = ctx.nameToIdx().get(baseName);
                        if (idx == null) continue;
                        int i1 = Math.min(idx + 1, ctx.stops().size() - 1);
                        int i2 = Math.min(idx + 2, ctx.stops().size() - 1);
                        String n1 = ctx.stops().get(i1).stationNameKo();
                        String n2 = ctx.stops().get(i2).stationNameKo();
                        String n1Norm = normalizeName(n1);
                        String n2Norm = normalizeName(n2);
                        if (!nextStationsNormNames.contains(n1Norm)) {
                            nextStationsNormNames.add(n1Norm);
                            normToOrigName.put(n1Norm, n1);
                        }
                        if (!nextStationsNormNames.contains(n2Norm)) {
                            nextStationsNormNames.add(n2Norm);
                            normToOrigName.put(n2Norm, n2);
                        }
                    }
                    if (nextStationsNormNames.isEmpty()) {
                        log.warn("[다음역 없음] 컨텍스트 부족 또는 매칭 실패");
                        return Mono.just(List.<TourPoiResponse.Item>of());
                    }
                    // 실제로는 seed = 좌표 쌍, 역명 중복 제거용이니 Set<String> → Seed 변환만 1회
                    List<Seed> seeds = nextStationsNormNames.stream()
                            .map(normToOrigName::get)
                            .map(this::resolveByNameFirst)
                            .filter(c -> !(c.lon == 0 && c.lat == 0))
                            .map(c -> new Seed(c.lon, c.lat))
                            .distinct()
                            .collect(Collectors.toList());
                    if (seeds.isEmpty()) return Mono.just(List.of());
                    return Flux.fromIterable(seeds)
                            .flatMap(seed ->
                                    tour.locationBasedList2(seed.lon, seed.lat, tourRadiusMeters, 1, pageSize, "C")
                                            .map(r -> Optional.ofNullable(r.response())
                                                    .map(TourPoiResponse.Resp::body)
                                                    .map(TourPoiResponse.Body::items)
                                                    .map(TourPoiResponse.Items::item)
                                                    .orElseGet(List::of))
                                            .doOnNext(list -> log.info("[TourAPI 위치기반] seed=({}, {}) 수신={}", seed.lat, seed.lon, list.size()))
                                            .flatMapIterable(i -> i)
                            )
                            .collect(Collectors.toMap(TourPoiResponse.Item::contentid, i -> i, (a, b) -> a, LinkedHashMap::new))
                            .map(map -> {
                                List<TourPoiResponse.Item> list = new ArrayList<>(map.values());
                                list.sort(Comparator.comparing(List1AllNearService::safeDist));
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
