package com.example.pp.rec.service.list1;


import com.example.pp.rec.config.TourApiV2Client;
import com.example.pp.rec.dto.TourPoiResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;



public class list1_tour_fetch {
    private final TourApiV2Client tour;

    public list1_tour_fetch(TourApiV2Client tour) { this.tour = tour; }

    // Seed 좌표로 관광지 조회 → contentid dedup → 거리(dist) 오름차순
    public Mono<List<TourPoiResponse.Item>> fetch(List<list1_models.Seed> seeds, int radius, int pageSize, String type) {
        return Flux.fromIterable(seeds)
                .flatMap(s -> tour.locationBasedList2(s.lon(), s.lat(), radius, 1, pageSize, "C", type)
                        .map(r -> Optional.ofNullable(r.response())
                                .map(TourPoiResponse.Resp::body)
                                .map(TourPoiResponse.Body::items)
                                .map(TourPoiResponse.Items::item)
                                .orElseGet(Collections::emptyList))
                        .flatMapIterable((List<TourPoiResponse.Item> i) -> i)
                )
                .collect(Collectors.toMap(
                        TourPoiResponse.Item::contentid,
                        Function.identity(),
                        (a,b)->a,
                        LinkedHashMap::new
                ))
                .map((Map<String, TourPoiResponse.Item> map) -> {
                    List<TourPoiResponse.Item> list = new ArrayList<>(map.values());
                    list.sort(Comparator.comparing(list1_orchestrator::safeDist));
                    return list;
                });
    }
}
