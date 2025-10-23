package com.example.pp.rec.service.list1;

import com.example.pp.data.service.NearbyPostgisService;
import com.example.pp.rec.dto.List1UserResponse;
import com.example.pp.rec.dto.TourPoiResponse;
import com.example.pp.rec.config.TourApiV2Client;
import com.example.pp.rec.entity.staion_info;
import com.example.pp.rec.repository.SubwayStationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.LocalTime;
import java.util.*;

@Service
public class list1_orchestrator {
    private static final Logger log = LoggerFactory.getLogger(list1_orchestrator.class);

    private final list1_nearby_stations nearby;
    private final list1_find_lines findLines;
    private final list1_line_contexts lineCtxs;
    private final list1_resolve_seeds resolver;
    private final list1_tour_fetch tourFetch;
    private final NearbyPostgisService nearbyAggregateService;


    public list1_orchestrator(

            list1_nearby_stations nearby,
            list1_find_lines findLines,
            list1_line_contexts lineCtxs,
            SubwayStationRepository stationRepo,
            TourApiV2Client tourClient,
            NearbyPostgisService nearbyAggregateService

    ) {

        this.nearby = nearby;
        this.findLines = findLines;
        this.lineCtxs = lineCtxs;
        this.resolver = new list1_resolve_seeds(stationRepo);
        this.tourFetch = new list1_tour_fetch(tourClient);
        this.nearbyAggregateService = nearbyAggregateService;
    }



    public Mono<List<Map<String,Object>>> build(double lat, double lon, LocalTime time,
                                                int tourRadiusMeters, int pageSize, List<String> categories) {
        final long t0 = System.nanoTime();
        return Mono.defer(() -> {
                    List<staion_info> nears = nearby.fetch(lat, lon);
                    if (nears.isEmpty()) return Mono.just(List.<Map<String,Object>>of()); // 빈 리스트도 제네릭 명시

                    final String dayType = "DAY";
                    final LocalTime start = time;
                    final LocalTime end = time.plusMinutes(10);

                    return findLines.find(nears, dayType, start, end)
                            .flatMap(found -> {
                                if (found.isEmpty()) return Mono.just(List.<Map<String,Object>>of());

                                Set<String> lineParams = found.stream()
                                        .map(list1_models.FoundLine::lineParam)
                                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

                                return lineCtxs.fetch(lineParams)
                                        .flatMap(lineCtxMap -> {

                                            var pair = list1_next_stations.pick(found, lineCtxMap);
                                            Map<String,String> normToOrig = pair.getKey();
                                            Map<String,String> normToLine = pair.getValue();
                                            if (normToOrig.isEmpty()) return Mono.just(List.<Map<String,Object>>of());

                                            List<list1_models.Seed> seeds = resolver.resolve(normToOrig, normToLine);
                                            if (seeds.isEmpty()) return Mono.just(List.<Map<String,Object>>of());

                                            return Mono.fromCallable(() -> {
                                                        List<Map<String,Object>> payload = new ArrayList<>();
                                                        int idx = 0;
                                                        for (var s : seeds) {
                                                            if (s.lon() == 0d && s.lat() == 0d) { idx++; continue; }
                                                            Map<String, List<?>> results =
                                                                    nearbyAggregateService.findAllEntitiesWithinRadius(s.lat(), s.lon(), tourRadiusMeters, categories);
                                                            Map<String, Object> wrapper = new LinkedHashMap<>();
                                                            wrapper.put("seedIndex", idx);
                                                            wrapper.put("seedLon", s.lon());
                                                            wrapper.put("seedLat", s.lat());
                                                            wrapper.put("results", results);
                                                            payload.add(wrapper);
                                                            idx++;
                                                        }
                                                        return payload;
                                                    })
                                                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
                                        });
                            });
                })
                .doOnError(e -> log.error("[List1] build failed: {}", e.toString(), e))
                .doFinally(type -> {
                    long ms = (System.nanoTime() - t0) / 1_000_000L;
                    log.info("[List1] build done signal={} elapsed={}ms", String.valueOf(type), ms);
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

    public reactor.core.publisher.Mono<List1UserResponse> buildResult(
            double lat, double lon, java.time.LocalTime time, int tourRadiusMeters, int pageSize, String type

    ) {

        final long t0 = System.nanoTime();
        return reactor.core.publisher.Mono.defer(() -> {

// 1) 주변역 조회(메모리 보관)
            java.util.List<staion_info> nears = nearby.fetch(lat, lon);
            if (nears.isEmpty()) {
                return reactor.core.publisher.Mono.just(
                        new List1UserResponse(time, time.plusMinutes(10), "DAY",
                                java.util.List.of(), java.util.List.of())

                );

            }



// 2) 시간창/일자

            final String dayType = "DAY";
            final java.time.LocalTime start = time;
            final java.time.LocalTime end = time.plusMinutes(10);



// 3) DB 선(역명+시간창) → 라인 확정

            return findLines.find(nears, dayType, start, end)
                    .flatMap(found -> {
                        if (found.isEmpty()) {
                            return reactor.core.publisher.Mono.just(
                                    new List1UserResponse(start, end, dayType,
                                            java.util.List.of(), java.util.List.of())

                            );

                        }



// 4) 라인 정차역 컨텍스트(라인별 정차역 + 인덱스 맵)

                        java.util.Set<String> lineParams = found.stream()
                                .map(list1_models.FoundLine::lineParam)
                                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));



                        return lineCtxs.fetch(lineParams)
                                .flatMap(lineCtxMap -> {
// 5) 각 라인별로 기준역 인덱스 → 다음/다다음역 이름과 좌표 계산
                                    java.util.List<List1UserResponse.LinePick> picks = new java.util.ArrayList<>();
                                    java.util.List<list1_models.Seed> seeds = new java.util.ArrayList<>();



                                    for (list1_models.FoundLine f : found) {
                                        list1_models.LineCtx ctx = lineCtxMap.get(f.lineParam());
                                        if (ctx == null || ctx.stops().isEmpty()) continue;



                                        String base = list1_models.ensureStationSuffix(f.stationNameKo());
                                        Integer idx = ctx.nameToIdx().get(list1_models.normalizeName(base));
                                        if (idx == null) continue;
                                        int i1 = Math.min(idx + 1, ctx.stops().size() - 1);
                                        int i2 = Math.min(idx + 2, ctx.stops().size() - 1);

                                        String n1 = list1_models.ensureStationSuffix(ctx.stops().get(i1).stationNameKo());
                                        String n2 = list1_models.ensureStationSuffix(ctx.stops().get(i2).stationNameKo());



// 좌표 해석: resolver.resolve를 단일 항목 맵으로 호출(라인+역명 우선)

                                        java.util.Map<String,String> nMap = new java.util.LinkedHashMap<>();
                                        java.util.Map<String,String> lMap = new java.util.LinkedHashMap<>();
                                        nMap.put(list1_models.normalizeName(n1), n1);
                                        lMap.put(list1_models.normalizeName(n1), f.humanLineName());
                                        java.util.List<list1_models.Seed> s1 = resolver.resolve(nMap, lMap);



                                        nMap.clear(); lMap.clear();
                                        nMap.put(list1_models.normalizeName(n2), n2);
                                        lMap.put(list1_models.normalizeName(n2), f.humanLineName());
                                        java.util.List<list1_models.Seed> s2 = resolver.resolve(nMap, lMap);



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

                                        if (lon1 != 0d || lat1 != 0d) seeds.add(new list1_models.Seed(lon1, lat1));
                                        if (lon2 != 0d || lat2 != 0d) seeds.add(new list1_models.Seed(lon2, lat2));

                                    }



// 다음/다다음역 이름/라인을 버퍼에도 보관(디버깅/재사용)

                                    java.util.Map<String,String> nameMap = new java.util.LinkedHashMap<>();
                                    java.util.Map<String,String> lineMap = new java.util.LinkedHashMap<>();

                                    for (List1UserResponse.LinePick p : picks) {
                                        nameMap.putIfAbsent(list1_models.normalizeName(p.next1().name()), p.next1().name());
                                        nameMap.putIfAbsent(list1_models.normalizeName(p.next2().name()), p.next2().name());
                                        lineMap.putIfAbsent(list1_models.normalizeName(p.next1().name()), p.humanLine());
                                        lineMap.putIfAbsent(list1_models.normalizeName(p.next2().name()), p.humanLine());

                                    }

// 6) 관광지 조회(필요 시 호출, 아니면 빈 리스트)

                                    if (seeds.isEmpty()) {
                                        return reactor.core.publisher.Mono.just(
                                                new List1UserResponse(start, end, dayType, picks, java.util.List.of())
                                        );

                                    }

                                    return tourFetch.fetch(seeds, tourRadiusMeters, pageSize, type)
                                            .defaultIfEmpty(java.util.List.of())
                                            .map(items -> new List1UserResponse(start, end, dayType, picks, items));

                                });

                    });

        }).doFinally(sig -> {

            long ms = (System.nanoTime() - t0) / 1_000_000L;
            org.slf4j.LoggerFactory.getLogger(list1_orchestrator.class)
                    .info("[List1] buildResult done (signal={}) elapsed={}ms", sig, ms);

        });

    }




}