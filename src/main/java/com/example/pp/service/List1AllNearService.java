package com.example.pp.service;




import com.example.pp.config.SeoulMetroClient;
import com.example.pp.config.TourApiV2Client;
import com.example.pp.dto.LineStationsResponse;
import com.example.pp.dto.TourPoiResponse;
import com.example.pp.entity.staion_info;
import com.example.pp.repository.SubwayStationRepository;
import com.example.pp.repository.TrainDataRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
public class List1AllNearService {

    private static final Logger log = LoggerFactory.getLogger(List1AllNearService.class);
    private static final double NEAR_RADIUS_M = 100.0;

    private final SubwayStationRepository stationRepo;
    private final SeoulMetroClient seoul;  // 노선별 정차역 API
    private final TourApiV2Client tour;    // 위치기반 관광지 API
    private final TrainDataRepository trainRepo; // 신규 주입

    // contentid → TourAPI Item
    private final Map<String, TourPoiResponse.Item> store = new LinkedHashMap<>();

    private record Seed(double lon, double lat) {}
    private record Coord(double lon, double lat) {}
    private record LineCtx(List<LineStationsResponse.Row> stops, Map<String,Integer> nameToIdx) {}

    public Mono<List<TourPoiResponse.Item>> build(double lat, double lon,  LocalTime time, int tourRadiusMeters, int pageSize, int type) {
        // 1) 주변역
        List<staion_info> nears = stationRepo.findAllWithinRadiusOrderByDistanceAsc(lat, lon);
        if (nears.isEmpty()) return Mono.just(List.of());

        // 1-1) 날짜 → dayType 매핑 (예: 평일=DAY, 토=SAT, 일=SUN)
        String dayType = mapDayType(java.time.LocalDate.now().getDayOfWeek());

        // 1-2) 시간창 정의(±15분). 자정 경계면 두 구간으로 나눠 검사.
        LocalTime start = time.minusMinutes(15);
        LocalTime end   = time.plusMinutes(15);

        // 1-3) 시간표 기반 필터링
        List<staion_info> filtered = nears.stream()
                .filter(st -> existsTrainAround(st, dayType, start, end))
                .toList();
        if (filtered.isEmpty()) return Mono.just(List.of());

        // 2) 필터된 라인만 정차역 조회
        java.util.Set<String> lines = filtered.stream()
                .map(staion_info::getLineName).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());

        return reactor.core.publisher.Flux.fromIterable(lines)
                .flatMap(line -> seoul.fetchLineStations1k(line).map(stops -> {
                    java.util.Map<String,Integer> nameToIdx = new java.util.HashMap<>();
                    for (int i=0;i<stops.size();i++) nameToIdx.putIfAbsent(normalizeName(stops.get(i).stationNameKo()), i);
                    return java.util.Map.entry(line, new LineCtx(stops, nameToIdx));
                }))
                .collectMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue)
                .flatMap(lineCtxMap -> {
                    // 3) 진행 방향 고려하여 인접 2역 선택
                    java.util.LinkedHashMap<String, String> normToOrigName = new java.util.LinkedHashMap<>();
                    java.util.LinkedHashMap<String, String> normToLine = new java.util.LinkedHashMap<>();

                    for (staion_info st : filtered) {
                        String line = st.getLineName();
                        LineCtx ctx = lineCtxMap.get(line);
                        if (ctx == null || ctx.stops().isEmpty()) continue;

                        Integer idx = ctx.nameToIdx().get(normalizeName(st.getStationName()));
                        if (idx == null) continue;

                        // 방향 조회(없으면 기본 DOWN 가정)
                        String dir = findDirection(st, dayType, time).orElse("DOWN");

                        int i1 = dir.equalsIgnoreCase("UP") ? Math.max(idx-1, 0) : Math.min(idx+1, ctx.stops().size()-1);
                        int i2 = dir.equalsIgnoreCase("UP") ? Math.max(idx-2, 0) : Math.min(idx+2, ctx.stops().size()-1);

                        String n1 = ctx.stops().get(i1).stationNameKo();
                        String n2 = ctx.stops().get(i2).stationNameKo();
                        String k1 = normalizeName(n1);
                        String k2 = normalizeName(n2);

                        normToOrigName.putIfAbsent(k1, n1);
                        normToOrigName.putIfAbsent(k2, n2);
                        normToLine.putIfAbsent(k1, line);
                        normToLine.putIfAbsent(k2, line);
                    }
                    if (normToOrigName.isEmpty()) return Mono.just(java.util.List.<TourPoiResponse.Item>of());

                    // 4) 좌표 해석 → Seed dedup → 관광지 수집(기존 로직 재사용)

                    // 4) (라인+역명) 좌표 해석 → Seed dedup(경도+위도)
                    List<Seed> seeds = normToOrigName.entrySet().stream()
                            .map(e -> {
                                String name = e.getValue();
                                String line = normToLine.getOrDefault(e.getKey(), "");
                                return resolveByLineAndNameFirst(line, name);
                            })
                            .filter(c -> !(c.lon==0 && c.lat==0))
                            .map(c -> new Seed(c.lon, c.lat))
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toMap(s -> s.lon+","+s.lat, Function.identity(), (a,b)->a, LinkedHashMap::new),
                                    m -> new ArrayList<>(m.values())
                            ));
                    if (seeds.isEmpty()) return Mono.just(List.of());

                    // 5) 위치기반 호출 → contentid로 dedup → dist 오름차순
                    return Flux.fromIterable(seeds)
                            .flatMap(seed ->
                                    tour.locationBasedList2(seed.lon, seed.lat, tourRadiusMeters, 1, pageSize, "C", type)
                                            .map(r -> Optional.ofNullable(r.response())
                                                    .map(TourPoiResponse.Resp::body)
                                                    .map(TourPoiResponse.Body::items)
                                                    .map(TourPoiResponse.Items::item)
                                                    .orElseGet(List::of))
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

    // (라인+역명)으로 단일 결과 우선 조회 → 실패 시 정규화 이름 Top1 → 실패 시 원문 이름 Top1
    private Coord resolveByLineAndNameFirst(String lineName, String stationName) {
        String normalized = normalizeName(stationName);
        return stationRepo.findTop1ByStationNameAndLineName(stationName, lineName)
                .or(() -> stationRepo.findTop1ByStationNameNormalized(normalized))
                .or(() -> stationRepo.findFirstByStationNameIgnoreCase(stationName))
                .or(() -> stationRepo.findOneByStationName(stationName))
                .map(s -> new Coord(s.getLon(), s.getLat()))
                .orElse(new Coord(0,0));
    }



    public List<TourPoiResponse.Item> getSavedList1() {
        return new ArrayList<>(store.values());
    }

    public void removeFromSavedList1(String contentId) {
        store.remove(contentId);
    }

    private static Double safeDist(TourPoiResponse.Item i) {
        String d = i.dist();
        if (d == null) return Double.POSITIVE_INFINITY;
        try { return Double.parseDouble(d); }
        catch (Exception e) { return Double.POSITIVE_INFINITY; }
    }
    private boolean existsTrainAround(staion_info st, String dayType, LocalTime start, LocalTime end) {
        // 자정 경계 분기
        if (start.isAfter(end)) {
            // [start, 23:59:59] OR [00:00:00, end]
            return trainRepo.existsByLineCodeAndStationNameAndDayTypeAndArrTimeBetween(
                    st.getStationId(), st.getStationName(), dayType, start, LocalTime.MAX)
                    || trainRepo.existsByLineCodeAndStationNameAndDayTypeAndArrTimeBetween(
                    st.getStationId(), st.getStationName(), dayType, LocalTime.MIN, end);
        }
        return trainRepo.existsByLineCodeAndStationNameAndDayTypeAndArrTimeBetween(
                st.getStationId(), st.getStationName(), dayType, start, end);
    }

    private java.util.Optional<String> findDirection(staion_info st, String dayType, LocalTime time) {
        return trainRepo.findTop1ByLineCodeAndStationNameAndDayTypeAndArrTimeGreaterThanEqualOrderByArrTimeAsc(
                        st.getStationId(), st.getStationName(), dayType, time)
                .map(com.example.pp.entity.train_data::getDirection);
    }

    private String mapDayType(java.time.DayOfWeek dow) {
        // 예시: 평일(DAY) / 토(SAT) / 일(SUN)
        return switch (dow) {
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
            default -> "DAY";
        };
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.replace("(", " ").replace(")", " ").replaceAll("\\s+", " ").trim();
    }
}
