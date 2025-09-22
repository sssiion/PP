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
        // 2) 라인별로 묶어 라인당 1회만 정차역 조회(5페이지×1000행 병합 반환)
        Map<String, List<staion_info>> byLine = nears.stream()
                .filter(s -> s.getLineName() != null)
                .collect(Collectors.groupingBy(staion_info::getLineName));

        // 2) 각 역의 노선 정차역 전체 조회 → 기준 역명 인덱스 → +1, +2 역명
        return Flux.fromIterable(byLine.keySet())
                .flatMap(line -> seoul.fetchLineStationsMax(line) // Mono<List<Row>>
                        .map(stops -> {
                            Map<String,Integer> nameToIdx = new HashMap<>();
                            for (int i = 0; i < stops.size(); i++) {
                                nameToIdx.putIfAbsent(normalizeName(stops.get(i).stationNameKo()), i);
                            }
                            return Map.entry(line, new LineCtx(stops, nameToIdx));
                        })
                        .doOnError(e -> log.error("[정차역 조회 실패] line={}, err={}", line, e.toString()))
                        .onErrorReturn(Map.entry(line, new LineCtx(List.of(), Map.of())))
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(lineCtxMap -> {
                    // 3) 각 근처역에 대해 “다음 2역” 이름을 추출(배열 순서 사용)
                    Set<String> nextNames = new LinkedHashSet<>();
                    for (staion_info st : nears) {
                        LineCtx ctx = lineCtxMap.get(st.getLineName());
                        if (ctx == null || ctx.stops().isEmpty()) continue;
                        Integer idx = ctx.nameToIdx().get(normalizeName(st.getStationName()));
                        if (idx == null) continue;
                        int i1 = Math.min(idx + 1, ctx.stops().size() - 1);
                        int i2 = Math.min(idx + 2, ctx.stops().size() - 1);
                        nextNames.add(ctx.stops().get(i1).stationNameKo());
                        nextNames.add(ctx.stops().get(i2).stationNameKo());
                    }
                    if (nextNames.isEmpty()) {
                        log.warn("[다음역 없음] 컨텍스트 부족 또는 매칭 실패");
                        return Mono.just(List.<TourPoiResponse.Item>of());
                    }

                    // 4) 역명 → 좌표 해석(이름 우선) → seed 좌표 집합
                    List<Seed> seeds = nextNames.stream()
                            .map(this::resolveByNameFirst)
                            .filter(c -> !(c.lon == 0 && c.lat == 0))
                            .map(c -> new Seed(c.lon, c.lat))
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toMap(s -> s.lon + "," + s.lat, Function.identity(), (a, b)->a, LinkedHashMap::new),
                                    m -> new ArrayList<>(m.values())
                            ));
                    if (seeds.isEmpty()) {
                        log.warn("[시드 좌표 없음] TourAPI 호출 생략");
                        return Mono.just(List.<TourPoiResponse.Item>of());
                    }

                    // 5) 좌표별 TourAPI 위치기반 호출 → contentid 중복 제거 → dist 오름차순
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
