package com.example.pp.service;


import com.example.pp.config.TourApiV2Client;

import com.example.pp.dto.TourPoiResponse;
import com.example.pp.util.Geo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttractionCollectService {

    private final TourApiV2Client tour;

    // 두 역 좌표로 수집 → contentId 중복 제거 → 거리 기준 정렬 → 메모리 저장/반환
    public Mono<List<Attraction>> collectFromTwoStations(double lon1, double lat1,
                                                         double lon2, double lat2,
                                                         int radiusMeters,
                                                        String type) {
        List<Seed> seeds = List.of(new Seed(lon1, lat1), new Seed(lon2, lat2));

        return Flux.fromIterable(seeds)
                .flatMap(s -> tour.locationBasedList2(s.lon, s.lat, radiusMeters, 1, 200, "C", type))
                .map(resp -> Optional.ofNullable(resp.response())
                        .map(TourPoiResponse.Resp::body)
                        .map(TourPoiResponse.Body::items)
                        .map(TourPoiResponse.Items::item)
                        .orElseGet(List::of))
                .flatMapIterable(it -> it)
                .collect(Collectors.toMap(
                        TourPoiResponse.Item::contentid,
                        i -> i,
                        (a,b)->a))
                .map(map -> {
                    List<Attraction> list = new ArrayList<>();
                    for (var i : map.values()) {
                        double d1 = Geo.haversineMeters(lat1, lon1, i.mapY(), i.mapX());
                        double d2 = Geo.haversineMeters(lat2, lon2, i.mapY(), i.mapX());
                        double min = Math.min(d1, d2);
                        list.add(new Attraction(
                                i.contentid(), i.contenttypeid(), i.title(), i.addr1(),
                                i.firstimage(), i.mapX(), i.mapY(), Math.round(min),
                                i.cat1(), i.cat2(), i.cat3()
                        ));
                    }
                    list.sort(Comparator.comparingDouble(Attraction::distanceMeters));
                    return list;
                });
    }

    public record Attraction(
            String contentId,
            String contentTypeId,
            String title,
            String addr1,
            String firstImage,
            Double mapX,
            Double mapY,
            double distanceMeters,
            String cat1,
            String cat2,
            String cat3
    ){}

    private record Seed(double lon, double lat){}
}
