package com.example.pp.service;



import com.example.pp.config.SeoulMetroClient;
import com.example.pp.config.TourApiV2Client;
import com.example.pp.dto.LineStationsResponse;
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
        log.info("[리스트3 시작] 위도={}, 경도={}, 시간={}, 윈도우(분)={}, 반경(m)={}, 페이지크기={}", lat, lon, time, windowMin, radius, pageSize);

        List<staion_info> near = stationRepo.findAllWithinRadiusOrderByDistanceAsc(lat, lon);
        if (near.isEmpty()) {
            log.warn("[근처 역 없음] 결과를 빈 리스트로 반환");
            return Mono.just(List.of());
        }
        staion_info base = near.get(0);

        // 2) 역명 → 리스트(모든 라인 고려 가능)
        Mono<List<SeoulMetroClient.StationSimpleDto>> byNameListMono = seoul.searchByStationName(base.getStationName());

        // 3) 한 라인만 예시(멀티 라인은 distinct 라인 순회)
        Mono<TwoSeeds> seedsMono = byNameListMono.flatMap(list -> {
            if (list.isEmpty()) {
                log.warn("[역명 검색 결과 없음] 시드 좌표를 생성할 수 없음");
                return Mono.just(new TwoSeeds(0,0,0,0));
            }
            var first = list.get(0);
            String line = first.lineNum();

            return seoul.fetchLineStationsMax(line).map(resp -> {
                var stops = Optional.ofNullable(resp.service())
                        .map(LineStationsResponse.ServiceBlock::rows)
                        .orElseGet(List::of);
                log.info("[노선 정차역 조회] 행 수={}", stops.size());

                record StopCoord(int idx, double latV, double lonV) {}
                List<StopCoord> coords = new ArrayList<>();
                for (int i=0; i<stops.size(); i++) {
                    var r = stops.get(i);
                    var c = resolveByNameFirst(r.stationNameKo(), r.stationCode());
                    if (!(c.lon()==0 && c.lat()==0)) coords.add(new StopCoord(i, c.lat(), c.lon()));
                }
                if (coords.isEmpty()) {
                    log.warn("[좌표 해석 실패] 시드 좌표를 생성할 수 없음");
                    return new TwoSeeds(0,0,0,0);
                }

                int nearestIdx = coords.stream()
                        .min(Comparator.comparingDouble(sc -> Geo.haversineMeters(lat, lon, sc.latV(), sc.lonV())))
                        .map(StopCoord::idx).orElse(0);

                int i1 = Math.min(nearestIdx + 1, stops.size() - 1);
                int i2 = Math.min(nearestIdx + 2, stops.size() - 1);

                var s1 = stops.get(i1);
                var s2 = stops.get(i2);

                var c1 = resolveByNameFirst(s1.stationNameKo(), s1.stationCode());
                var c2 = resolveByNameFirst(s2.stationNameKo(), s2.stationCode());

                log.info("[시드 좌표 결정] 기준Idx={}, 다음Idx들=({}, {}), 시드1=({}, {}), 시드2=({}, {})",
                        nearestIdx, i1, i2, c1.lat(), c1.lon(), c2.lat(), c2.lon());
                return new TwoSeeds(c1.lon(), c1.lat(), c2.lon(), c2.lat());
            });
        });

        Mono<List<LocationPoi>> list1Mono = seedsMono.flatMap(seeds -> {
            if (seeds.empty()) {
                log.warn("[시드 좌표 없음] 위치기반 수집을 건너뜀");
                return Mono.just(List.of());
            }
            return Flux.just(new Seed(seeds.lon1, seeds.lat1), new Seed(seeds.lon2, seeds.lat2))
                    .flatMap(s -> tour.locationBasedList2(s.lon, s.lat, radius, 1, 200, "C"))
                    .map(r -> Optional.ofNullable(r.response()).map(TourPoiResponse.Resp::body)
                            .map(TourPoiResponse.Body::items).map(TourPoiResponse.Items::item).orElseGet(List::of))
                    .doOnNext(list -> log.info("[위치기반 수집] 추가 수신 건수={}", list.size()))
                    .flatMapIterable(i -> i)
                    .collect(Collectors.toMap(TourPoiResponse.Item::contentid, i -> i, (a,b)->a))
                    .map(map -> map.values().stream().map(i -> {
                        double d1 = Geo.haversineMeters(seeds.lat1, seeds.lon1, i.mapY(), i.mapX());
                        double d2 = Geo.haversineMeters(seeds.lat2, seeds.lon2, i.mapY(), i.mapX());
                        return new LocationPoi(i.contentid(), i.contenttypeid(), i.title(), i.addr1(),
                                i.firstimage(), i.mapX(), i.mapY(), Math.min(d1, d2));
                    }).collect(Collectors.toList()));
        });

        Mono<List<AreaCollectService.AreaPoi>> list2Mono = areaCollect.collectAllSeoul(pageSize)
                .doOnSuccess(list -> log.info("[지역기반 수집(서울)] 총 건수={}", list.size()));

        return Mono.zip(list1Mono, list2Mono).map(tuple -> {
            var loc = tuple.getT1();
            var area = tuple.getT2();
            Set<String> areaIds = area.stream().map(AreaCollectService.AreaPoi::contentId).collect(Collectors.toSet());
            return loc.stream()
                    .filter(p -> areaIds.contains(p.contentId))
                    .map(p -> new List3Item(p.contentId, p.contentTypeId, p.title, p.addr1, p.firstImage,
                            p.mapX, p.mapY, Math.round(p.distanceMeters)))
                    .collect(Collectors.toList());
        });
    }

    private Coord resolveByNameFirst(String stationName, String fallbackStationCode) {
        String normalized = normalizeName(stationName);
        return stationRepo.findOneByStationName(stationName)
                .or(() -> stationRepo.findFirstByStationNameIgnoreCase(stationName))
                .or(() -> stationRepo.findOneByStationNameNormalized(normalized))
                .or(() -> stationRepo.findOneByStationId(fallbackStationCode))
                .map(s -> new Coord(s.getLon(), s.getLat()))
                .orElse(new Coord(0,0));
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.replace("(", " ").replace(")", " ").replaceAll("\\s+", " ").trim();
    }

    private record Seed(double lon, double lat) {}
    private record Coord(double lon, double lat) {}
    private record TwoSeeds(double lon1, double lat1, double lon2, double lat2) {
        boolean empty(){ return lon1==0 && lat1==0 && lon2==0 && lat2==0; }
    }
    private record LocationPoi(String contentId, String contentTypeId, String title, String addr1,
                               String firstImage, Double mapX, Double mapY, double distanceMeters){}
    public record List3Item(String contentId, String contentTypeId, String title, String addr1,
                            String firstImage, Double mapX, Double mapY, double distanceMeters){}
    // 1,2,3,4 순서 그대로 보존
    public record StationInputDto(
            String stationName,   // 1
            double longitude,     // 2
            String lineName,      // 3  (ex: "01호선", "GTX-A" 등 OA-15442 LINE_NUM과 일치)
            double latitude       // 4
    ) {}

    // 위치기반 관광지 결과(리스트1용 최소 필드)
    public record List1Poi(
            String contentId,   // contentid [응답 키]
            String title,       // title
            String addr1,       // addr1
            Double mapX,        // 경도(mapx)
            Double mapY,        // 위도(mapy)
            Long dist,          // 거리(dist) - 미터, 반올림 저장
            String firstImage   // firstimage
    ) {}
}

