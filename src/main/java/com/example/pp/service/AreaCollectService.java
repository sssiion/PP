package com.example.pp.service;


import com.example.pp.config.TourApiV2Client;

import com.example.pp.dto.TourPoiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaCollectService {

    private final TourApiV2Client tour;

    public Mono<List<AreaPoi>> collectAllSeoul(int pageSize) {

        return tour.areaBasedList2Seoul()
                .flatMap(first -> {
                    int total = Optional.ofNullable(first.response())
                            .map(TourPoiResponse.Resp::body)
                            .map(TourPoiResponse.Body::totalCount)
                            .orElse(0); // totalCount 없으면 0 [web:458]

                    int pages = (int) Math.ceil(total / (double) pageSize);

                    // 누적 맵(contentId 중복 제거)
                    Map<String, TourPoiResponse.Item> acc = new LinkedHashMap<>();
                    putAll(acc, first);

                    if (pages <= 1) {
                        return Mono.just(toAreaPoiList(acc));
                    }

                    // 2..pages 페이지를 Flux로 병합 후 모두 처리
                    return Flux.range(2, pages - 1)
                            .flatMap(p -> tour.areaBasedList2Seoul()) // 병렬 수집 [web:481]
                            .doOnNext(r -> putAll(acc, r))
                            .then(Mono.fromSupplier(() -> toAreaPoiList(acc))); // 완료 후 리스트 생성 [web:468]
                });
    }

    private List<AreaPoi> toAreaPoiList(Map<String, TourPoiResponse.Item> acc) {
        return acc.values().stream().map(i -> new AreaPoi(
                i.contentid(), i.contenttypeid(), i.title(), i.addr1(),
                i.firstimage(), i.mapX(), i.mapY(),
                i.areacode(), i.sigungucode(), i.cat1(), i.cat2(), i.cat3()
        )).collect(Collectors.toList());
    }

    private void putAll(Map<String, TourPoiResponse.Item> acc, TourPoiResponse resp) {
        var items = Optional.ofNullable(resp.response())
                .map(TourPoiResponse.Resp::body)
                .map(TourPoiResponse.Body::items)
                .map(TourPoiResponse.Items::item)
                .orElseGet(List::of);
        for (var i : items) acc.putIfAbsent(i.contentid(), i);
    }

    public record AreaPoi(
            String contentId,
            String contentTypeId,
            String title,
            String addr1,
            String firstImage,
            Double mapX,
            Double mapY,
            String areaCode,
            String sigunguCode,
            String cat1,
            String cat2,
            String cat3
    ) {}
}
