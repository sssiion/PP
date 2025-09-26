package com.example.pp.service.list1;


import com.example.pp.dto.List1UserResponse;
import com.example.pp.dto.TourPoiResponse;
import com.example.pp.entity.BusStop;
import com.example.pp.entity.staion_info;
import com.example.pp.repository.BusStopRepository;
import com.example.pp.repository.SubwayStationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class list1_orchestrator {

    private static final Logger log = LoggerFactory.getLogger(list1_orchestrator.class);

    private final list1_nearby_stations nearby;
    private final list1_find_lines findLines;
    private final list1_line_contexts lineCtxs;
    private final list1_resolve_seeds resolver;
    private final list1_tour_fetch tourFetch;
    private final list1_runtime_buffer buffer;
    private final BusStopRepository busStopRepository;

    public list1_orchestrator(
            list1_nearby_stations nearby,
            list1_find_lines findLines,
            list1_line_contexts lineCtxs,
            SubwayStationRepository stationRepo,
            com.example.pp.config.TourApiV2Client tourClient,
            list1_runtime_buffer buffer, BusStopRepository busStopRepository
    ) {
        this.nearby = nearby;
        this.findLines = findLines;
        this.lineCtxs = lineCtxs;
        this.resolver = new list1_resolve_seeds(stationRepo);
        this.tourFetch = new list1_tour_fetch(tourClient);
        this.buffer = buffer;
        this.busStopRepository = busStopRepository;
    }

    public Mono<List<TourPoiResponse.Item>> build(double lat, double lon, LocalTime time,
                                                  int tourRadiusMeters, int pageSize, String type) {
        final long t0 = System.nanoTime();
        return Mono.defer(() -> {
            // 1) 주변역
            List<staion_info> nears = nearby.fetch(lat, lon);
            buffer.setNearbyStations(nears); // 메모리에 보관
            if (nears.isEmpty()) return Mono.<List<TourPoiResponse.Item>>just(java.util.Collections.emptyList());

            // 2) 시간창/일자
            final String dayType = "DAY";
            final LocalTime start = time;
            final LocalTime end   = time.plusMinutes(10);

            // 3) DB 선 + API 매핑
            return findLines.find(nears, dayType, start, end)
                    .flatMap(found -> {
                        buffer.setFoundLines(found); // 보관
                        if (found.isEmpty()) return Mono.<List<TourPoiResponse.Item>>just(java.util.Collections.emptyList());

                        // 4) 라인 정차역 컨텍스트
                        java.util.Set<String> lineParams = found.stream().map(list1_models.FoundLine::lineParam)
                                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
                        return lineCtxs.fetch(lineParams)
                                .flatMap(lineCtxMap -> {
                                    buffer.setLineCtxMap(lineCtxMap); // 보관

                                    // 5) 다음/다다음역
                                    var pair = list1_next_stations.pick(found, lineCtxMap);
                                    java.util.Map<String, String> normToOrig = pair.getKey();
                                    java.util.Map<String, String> normToLine = pair.getValue();
                                    buffer.setNextStations(normToOrig, normToLine); // 보관
                                    if (normToOrig.isEmpty())
                                        return Mono.<List<TourPoiResponse.Item>>just(java.util.Collections.emptyList());

                                    // 6) 좌표 시드
                                    java.util.List<list1_models.Seed> seeds = resolver.resolve(normToOrig, normToLine);
                                    buffer.setSeeds(seeds); // 보관
                                    if (seeds.isEmpty())
                                        return Mono.<List<TourPoiResponse.Item>>just(java.util.Collections.emptyList());

                                    // 7) 관광지 조회
                                    return tourFetch.fetch(seeds, tourRadiusMeters, pageSize, type)
                                            .map(list -> {
                                                // Map<contentid, Item>으로도 메모리에 저장(원본 그대로)
                                                java.util.Map<String, TourPoiResponse.Item> map = list.stream()
                                                        .collect(java.util.stream.Collectors.toMap(
                                                                TourPoiResponse.Item::contentid,
                                                                java.util.function.Function.identity(),
                                                                (a,b)->a,
                                                                java.util.LinkedHashMap::new
                                                        ));
                                                buffer.setTourItems(map); // 보관
                                                return list;
                                            });
                                });
                    });
        }).doFinally(sig -> {
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            org.slf4j.LoggerFactory.getLogger(list1_orchestrator.class).info("[List1] done (signal={}) elapsed={}ms", sig, ms);
        });
    }

    // 거리 파싱 공용
    public static Double safeDist(TourPoiResponse.Item i) {
        String d = i.dist();
        if (d == null) return Double.POSITIVE_INFINITY;
        try { return Double.parseDouble(d); }
        catch (Exception e) { return Double.POSITIVE_INFINITY; }
    }
    // list1_orchestrator 내부에 추가
    public reactor.core.publisher.Mono<com.example.pp.dto.List1UserResponse> buildResult(
            double lat, double lon, java.time.LocalTime time,
            int stopSearchRadiusMeters, // [추가] 버스 정류장 검색 반경 파라미터
            int tourRadiusMeters, int pageSize, String type
    ) {
        final long t0 = System.nanoTime();
        return reactor.core.publisher.Mono.defer(() -> {
            // 1) 주변역 조회(메모리 보관)
            java.util.List<com.example.pp.entity.staion_info> nears = nearby.fetch(lat, lon);
            buffer.setNearbyStations(nears);
            if (nears.isEmpty()) {
                return reactor.core.publisher.Mono.just(
                        new com.example.pp.dto.List1UserResponse(time, time.plusMinutes(10), "DAY",
                                java.util.List.of(), java.util.List.of())
                );
            }

            // 2) 시간창/일자
            final String dayType = "DAY";
            final java.time.LocalTime start = time;
            final java.time.LocalTime end   = time.plusMinutes(10);

            // 3) DB 선(역명+시간창) → 라인 확정
            return findLines.find(nears, dayType, start, end)
                    .flatMap(found -> {
                        buffer.setFoundLines(found);
                        if (found.isEmpty()) {
                            return reactor.core.publisher.Mono.just(
                                    new com.example.pp.dto.List1UserResponse(start, end, dayType,
                                            java.util.List.of(), java.util.List.of())
                            );
                        }

                        // 4) 라인 정차역 컨텍스트(라인별 정차역 + 인덱스 맵)
                        java.util.Set<String> lineParams = found.stream()
                                .map(com.example.pp.service.list1.list1_models.FoundLine::lineParam)
                                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

                        return lineCtxs.fetch(lineParams)
                                .flatMap(lineCtxMap -> {
                                    buffer.setLineCtxMap(lineCtxMap);

                                    // 5) 각 라인별로 기준역 인덱스 → 다음/다다음역 이름과 좌표 계산
                                    java.util.List<List1UserResponse.LinePick> picks = new java.util.ArrayList<>();
                                    java.util.List<com.example.pp.service.list1.list1_models.Seed> seeds = new java.util.ArrayList<>();

                                    for (com.example.pp.service.list1.list1_models.FoundLine f : found) {
                                        com.example.pp.service.list1.list1_models.LineCtx ctx = lineCtxMap.get(f.lineParam());
                                        if (ctx == null || ctx.stops().isEmpty()) continue;

                                        String base = com.example.pp.service.list1.list1_models.ensureStationSuffix(f.stationNameKo());
                                        Integer idx = ctx.nameToIdx().get(com.example.pp.service.list1.list1_models.normalizeName(base));
                                        if (idx == null) continue;

                                        int i1 = Math.min(idx + 1, ctx.stops().size() - 1);
                                        int i2 = Math.min(idx + 2, ctx.stops().size() - 1);

                                        String n1 = com.example.pp.service.list1.list1_models.ensureStationSuffix(ctx.stops().get(i1).stationNameKo());
                                        String n2 = com.example.pp.service.list1.list1_models.ensureStationSuffix(ctx.stops().get(i2).stationNameKo());

                                        // 좌표 해석: resolver.resolve를 단일 항목 맵으로 호출(라인+역명 우선)
                                        java.util.Map<String,String> nMap = new java.util.LinkedHashMap<>();
                                        java.util.Map<String,String> lMap = new java.util.LinkedHashMap<>();
                                        nMap.put(com.example.pp.service.list1.list1_models.normalizeName(n1), n1);
                                        lMap.put(com.example.pp.service.list1.list1_models.normalizeName(n1), f.humanLineName());
                                        java.util.List<com.example.pp.service.list1.list1_models.Seed> s1 = resolver.resolve(nMap, lMap);

                                        nMap.clear(); lMap.clear();
                                        nMap.put(com.example.pp.service.list1.list1_models.normalizeName(n2), n2);
                                        lMap.put(com.example.pp.service.list1.list1_models.normalizeName(n2), f.humanLineName());
                                        java.util.List<com.example.pp.service.list1.list1_models.Seed> s2 = resolver.resolve(nMap, lMap);

                                        Double lon1 = (s1.isEmpty() ? 0d : s1.get(0).lon());
                                        Double lat1 = (s1.isEmpty() ? 0d : s1.get(0).lat());
                                        Double lon2 = (s2.isEmpty() ? 0d : s2.get(0).lon());
                                        Double lat2 = (s2.isEmpty() ? 0d : s2.get(0).lat());

                                        List1UserResponse.NextStation next1 = new List1UserResponse.NextStation(n1, lon1, lat1);
                                        List1UserResponse.NextStation next2 = new List1UserResponse.NextStation(n2, lon2, lat2);

                                        picks.add(new List1UserResponse.LinePick(
                                                f.lineParam(), f.humanLineName(), base, next1, next2
                                        ));

                                        // 관광지 조회용 시드(0,0은 추가 안 함)
                                        if (lon1 != 0d || lat1 != 0d) seeds.add(new com.example.pp.service.list1.list1_models.Seed(lon1, lat1));
                                        if (lon2 != 0d || lat2 != 0d) seeds.add(new com.example.pp.service.list1.list1_models.Seed(lon2, lat2));
                                    }

                                    // [추가] 현재 위치 기반 주변 버스 정류장 조회 및 Seed 리스트에 추가
                                    log.info("사용자 위치 ({}, {}) 근처 {}m 내 버스 정류장 조회를 시작합니다.", lat, lon, stopSearchRadiusMeters);
                                    List<BusStop> nearbyBusStops = busStopRepository.findBusStopsWithinRadius(lat, lon, stopSearchRadiusMeters);
                                    log.info("총 {}개의 주변 버스 정류장을 찾았습니다. 관광지 검색 시드에 추가합니다.", nearbyBusStops.size());

                                    nearbyBusStops.forEach(stop ->
                                            seeds.add(new com.example.pp.service.list1.list1_models.Seed(stop.getLongitude(), stop.getLatitude()))
                                    );

                                    // [추가] 지하철 + 버스 Seed 리스트에서 중복된 좌표를 제거
                                    List<list1_models.Seed> distinctSeeds = seeds.stream()
                                            .distinct()
                                            .collect(Collectors.toList());


                                    buffer.setSeeds(distinctSeeds);
                                    // 다음/다다음역 이름/라인을 버퍼에도 보관(디버깅/재사용)
                                    java.util.Map<String,String> nameMap = new java.util.LinkedHashMap<>();
                                    java.util.Map<String,String> lineMap = new java.util.LinkedHashMap<>();
                                    for (List1UserResponse.LinePick p : picks) {
                                        nameMap.putIfAbsent(com.example.pp.service.list1.list1_models.normalizeName(p.next1().name()), p.next1().name());
                                        nameMap.putIfAbsent(com.example.pp.service.list1.list1_models.normalizeName(p.next2().name()), p.next2().name());
                                        lineMap.putIfAbsent(com.example.pp.service.list1.list1_models.normalizeName(p.next1().name()), p.humanLine());
                                        lineMap.putIfAbsent(com.example.pp.service.list1.list1_models.normalizeName(p.next2().name()), p.humanLine());
                                    }
                                    buffer.setNextStations(nameMap, lineMap);
                                    buffer.setSeeds(seeds);

                                    // 6) 관광지 조회(필요 시 호출, 아니면 빈 리스트)
                                    if (distinctSeeds.isEmpty()) {
                                        return reactor.core.publisher.Mono.just(
                                                new com.example.pp.dto.List1UserResponse(start, end, dayType, picks, java.util.List.of())
                                        );
                                    }
                                    return tourFetch.fetch(distinctSeeds, tourRadiusMeters, pageSize, type)
                                            .defaultIfEmpty(java.util.List.of())
                                            .map(items -> {
                                                // 버퍼에도 저장(한번 쓰고 버리는 용도)
                                                java.util.Map<String, com.example.pp.dto.TourPoiResponse.Item> map =
                                                        items.stream().collect(java.util.stream.Collectors.toMap(
                                                                com.example.pp.dto.TourPoiResponse.Item::contentid,
                                                                java.util.function.Function.identity(),
                                                                (a,b)->a,
                                                                java.util.LinkedHashMap::new
                                                        ));
                                                buffer.setTourItems(map);
                                                return new com.example.pp.dto.List1UserResponse(start, end, dayType, picks, items);
                                            });
                                });
                    });
        }).doFinally(sig -> {
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            org.slf4j.LoggerFactory.getLogger(list1_orchestrator.class)
                    .info("[List1] buildResult done (signal={}) elapsed={}ms", sig, ms);
        });
    }

}
