package com.example.pp.service;


import com.example.pp.config.SeoulMetroClient;
import com.example.pp.config.TourApiV2Client;
import com.example.pp.dto.LineStationsResponse;
import com.example.pp.dto.SeoulWrap;
import com.example.pp.dto.StationByName;

import com.example.pp.dto.TourPoiResponse;
import com.example.pp.entity.staion_info;
import com.example.pp.repository.SubwayStationRepository;
import com.example.pp.util.Geo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class List3BuildService {
    private static final Logger log = LoggerFactory.getLogger(List3BuildService.class);
    private final SubwayStationRepository stationRepo;
    private final SeoulMetroClient seoul;
    private final TourApiV2Client tour;
    private final AreaCollectService areaCollect;

    public Mono<List<List3Item>> build(double lat, double lon, LocalTime time,
                                       int windowMin, int radius, int pageSize) {
        log.info("[리스트3 시작] 위도={}, 경도={}, 시간={}, 윈도우(분)={}, 반경(m)={}, 페이지크기={}",
                lat, lon, time, windowMin, radius, pageSize);
        // 1) 근접 50m 역 중 가장 가까운 1개
        List<staion_info> near = stationRepo.findAllWithinRadiusOrderByDistanceAsc(lat, lon, 50.0);
        if (near.isEmpty()) {log.warn("[근처 역 없음] 결과를 빈 리스트로 반환");return Mono.just(List.of());}
        staion_info base = near.get(0);

        // 2) 역명→역코드/호선
        Mono<SeoulWrap<StationByName.Item>> byNameMono = seoul.searchByStationName(base.getStationName())
                .doOnSuccess(w -> {
                    int rows = Optional.ofNullable(w.SearchInfoBySubwayNameService())
                            .map(SeoulWrap.Result::row).map(List::size).orElse(0);
                    log.info("[역명 검색 완료] 행 수={}", rows); // 역명 검색 로그 [web:26]
                });

        // 3) 라인별 역 목록 가져와 기준 역 다음 2개 역을 시드로 선택(단순 전개)
        Mono<TwoSeeds> seedsMono = byNameMono.flatMap(wrap -> {
            var rows = Optional.ofNullable(wrap.SearchInfoBySubwayNameService())
                    .map(SeoulWrap.Result::row).orElseGet(List::of);
            if (rows.isEmpty()) {log.warn("[역명 검색 결과 없음] 시드 좌표를 생성할 수 없음"); return Mono.just(new TwoSeeds(0,0,0,0)); }
            var first = rows.get(0);
            String line = first.lineNum();
            //String stationCd = first.stationCode();
            // 노선 전체 역
            return seoul.fetchLineStationsMax(line).map(resp -> {
                var stops = Optional.ofNullable(resp.service()).map(LineStationsResponse.ServiceBlock::rows)
                        .orElseGet(List::of);
                log.info("[노선 정차역 조회] 행 수={}", stops.size());
                // 1) 정차역 좌표 해석
                record StopCoord(int idx, double lat, double lon) {}
                List<StopCoord> coords = new ArrayList<>();
                for (int i=0; i<stops.size(); i++) {
                    var r = stops.get(i);
                    var c = resolveCoord(r.stationCode(), r.stationNameKo());
                    if (!(c.lon()==0 && c.lat()==0)) coords.add(new StopCoord(i, c.lat(), c.lon()));
                }
                if (coords.isEmpty()) {
                    log.warn("[좌표 해석 실패] 시드 좌표를 생성할 수 없음");
                    return new TwoSeeds(0,0,0,0);
                }

                // 2) 현재 위치에서 가장 가까운 역 인덱스 선택
                int nearestIdx = coords.stream()
                        .min(Comparator.comparingDouble(sc -> Geo.haversineMeters(
                                /* current */ lat, lon, sc.lat(), sc.lon())))
                        .map(StopCoord::idx).orElse(0);

                // 3) 아래 2개 역(경계 클램프)
                int i1 = Math.min(nearestIdx + 1, stops.size() - 1);
                int i2 = Math.min(nearestIdx + 2, stops.size() - 1);

                var s1 = stops.get(i1);
                var s2 = stops.get(i2);
                var c1 = resolveCoord(s1.stationCode(), s1.stationNameKo());
                var c2 = resolveCoord(s2.stationCode(), s2.stationNameKo());

                log.info("[시드 좌표 결정] 기준Idx={}, 다음Idx들=({}, {}), 시드1=({}, {}), 시드2=({}, {})",
                        nearestIdx, i1, i2, c1.lat(), c1.lon(), c2.lat(), c2.lon());
                return new TwoSeeds(c1.lon(), c1.lat(), c2.lon(), c2.lat());
            });
        });

        // 4) 두 역 좌표로 위치기반 수집(리스트1)
        Mono<List<LocationPoi>> list1Mono = seedsMono.flatMap(seeds -> {
            if (seeds.empty()) {log.warn("[시드 좌표 없음] 위치기반 수집을 건너뜀");  return Mono.just(List.of());}
            return Flux.just(new Seed(seeds.lon1, seeds.lat1), new Seed(seeds.lon2, seeds.lat2))
                    .flatMap(s -> tour.locationBasedList2(s.lon, s.lat, radius, 1, 200, "C"))
                    .map(r -> Optional.ofNullable(r.response()).map(TourPoiResponse.Resp::body)
                            .map(TourPoiResponse.Body::items).map(TourPoiResponse.Items::item).orElseGet(List::of))
                    .doOnNext(list -> log.info("[위치기반 수집] 추가 수신 건수={}", list.size()))
                    .flatMapIterable(i -> i)
                    .collect(Collectors.toMap(TourPoiResponse.Item::contentid, i -> i, (a,b)->a))
                    .map(map ->

                            map.values().stream().map(i -> {
                        double d1 = Geo.haversineMeters(seeds.lat1, seeds.lon1, i.mapY(), i.mapX());
                        double d2 = Geo.haversineMeters(seeds.lat2, seeds.lon2, i.mapY(), i.mapX());
                        return new LocationPoi(
                                i.contentid(), i.contenttypeid(), i.title(), i.addr1(),
                                i.firstimage(), i.mapX(), i.mapY(), Math.min(d1, d2)
                        );
                    }).collect(Collectors.toList()));
        });

        // 5) 서울 지역기반 전량 수집(리스트2)
        Mono<List<AreaCollectService.AreaPoi>> list2Mono = areaCollect.collectAllSeoul(pageSize)
                .doOnSuccess(list -> log.info("[지역기반 수집(서울)] 총 건수={}", list.size()));

        // 6) contentId 교집합 → 리스트3
        return Mono.zip(list1Mono, list2Mono).map(tuple -> {
            var loc = tuple.getT1();
            var area = tuple.getT2();
            Set<String> areaIds = area.stream().map(AreaCollectService.AreaPoi::contentId).collect(Collectors.toSet());
            return loc.stream()
                    .filter(p -> areaIds.contains(p.contentId))
                    .map(p -> new List3Item(
                            p.contentId, p.contentTypeId, p.title, p.addr1, p.firstImage,
                            p.mapX, p.mapY, Math.round(p.distanceMeters)
                    ))
                    .collect(Collectors.toList());
        });
    }

    // ---- 보조 구조 및 유틸 ----

    private Coord resolveCoord(String stationCode, String stationName) {
        return stationRepo.findOneByStationId(stationCode)
                .or(() -> stationRepo.findOneByStationName(stationName))
                .map(s -> new Coord(s.getLon(), s.getLat()))
                .orElse(new Coord(0,0));
    }

    private static String dayTag(DayOfWeek dow) {
        return switch (dow) { case SATURDAY -> "2"; case SUNDAY -> "3"; default -> "1"; };
    }

    private record Seed(double lon, double lat) {}
    private record Coord(double lon, double lat) {}
    private record TwoSeeds(double lon1, double lat1, double lon2, double lat2) {
        boolean empty(){ return lon1==0 && lat1==0 && lon2==0 && lat2==0; }
    }
    private record LocationPoi(
            String contentId, String contentTypeId, String title, String addr1,
            String firstImage, Double mapX, Double mapY, double distanceMeters
    ){}
    public record List3Item(
            String contentId, String contentTypeId, String title, String addr1,
            String firstImage, Double mapX, Double mapY, double distanceMeters
    ){}
}
