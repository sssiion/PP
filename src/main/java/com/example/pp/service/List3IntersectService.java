package com.example.pp.service;



import com.example.pp.dto.TourPoiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * list1: 위치기반(locationBasedList2)에서 모은 아이템 목록
 * list2: 지역기반(areaBasedList2, areaCode=1)에서 모은 아이템 목록
 * 결과: 두 목록의 contentId 교집합을 리스트3로 저장/반환
 */
@Service
@RequiredArgsConstructor
public class List3IntersectService {

    // 간단한 메모리 저장소(필요 시 캐시/DB로 교체)
    private final Map<String, List3Item> store = new LinkedHashMap<>();

    // 위치기반/지역기반 모두 같은 DTO(TourPoiResponse.Item) 사용
    public Mono<List<List3Item>> buildAndStore(
            Mono<List<TourPoiResponse.Item>> locationItemsMono,
            Mono<List<TourPoiResponse.Item>> areaItemsMono,
            Map<String, Double> distanceByContentId // 선택: 위치기반에서 계산된 거리(m) 전달
    ) {
        return Mono.zip(locationItemsMono, areaItemsMono).map(tuple -> {
            var loc = Optional.ofNullable(tuple.getT1()).orElseGet(List::of);
            var area = Optional.ofNullable(tuple.getT2()).orElseGet(List::of);

            // 지역기반 contentId 집합
            Set<String> areaIds = area.stream()
                    .map(TourPoiResponse.Item::contentid)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 교집합 생성(위치기반 중 area에도 존재하는 것)
            List<List3Item> list3 = loc.stream()
                    .filter(i -> i.contentid() != null && areaIds.contains(i.contentid()))
                    .map(i -> new List3Item(
                            i.contentid(),
                            i.contenttypeid(),
                            i.title(),
                            i.addr1(),
                            i.firstimage(),
                            i.mapX(),
                            i.mapY(),
                            Optional.ofNullable(distanceByContentId).map(m -> m.get(i.contentid())).orElse(null),
                            i.cat1(),
                            i.cat2(),
                            i.cat3()
                    ))
                    // contentId 중복 방지
                    .collect(Collectors.toMap(List3Item::contentId, it -> it, (a,b)->a))
                    .values().stream().collect(Collectors.toList());

            // 메모리 저장소 갱신
            list3.forEach(item -> store.put(item.contentId(), item));

            return list3;
        });
    }

    // 저장소에서 모두 가져오기
    public List<List3Item> getAllSaved() {
        return new ArrayList<>(store.values());
    }

    public record List3Item(
            String contentId,
            String contentTypeId,
            String title,
            String addr1,
            String firstImage,
            Double mapX,
            Double mapY,
            Double distanceMeters, // 위치기반에서 계산했을 경우만 채움
            String cat1,
            String cat2,
            String cat3
    ) {}
}
