package com.example.pp.service;


import com.example.pp.dto.TourPoiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * list1: 위치기반(locationBasedList2)에서 모은 아이템 목록 (TourPoiResponse.Item 원본)
 * list2: 지역기반(areaBasedList2, areaCode=1)에서 모은 contentid "리스트만"
 * 결과: contentid 교집합을 리스트3로 저장/반환
 */
@Service
@RequiredArgsConstructor
public class List3IntersectService {

    // 간단한 메모리 저장소(필요 시 캐시/DB로 교체)
    private final Map<String, List3Item> store = new LinkedHashMap<>();

    /**
     * @param locationItemsMono List1: TourAPI Item 원본 리스트
     * @param areaContentIdsMono List2: contentid 리스트(지역기반 전체 중 서울)
     * @param distanceByContentId 선택: 위치기반에서 계산된 거리(m) 전달(없으면 item.dist 파싱으로 보조)
     */
    public Mono<List<List3Item>> buildAndStore(
            Mono<List<TourPoiResponse.Item>> locationItemsMono,
            Mono<List<String>> areaContentIdsMono,
            Map<String, Double> distanceByContentId
    ) {
        return Mono.zip(locationItemsMono, areaContentIdsMono).map(tuple -> {
            var loc = Optional.ofNullable(tuple.getT1()).orElseGet(List::of);
            var areaIds = new HashSet<>(Optional.ofNullable(tuple.getT2()).orElseGet(List::of));

            // 교집합 생성: 위치기반 중에서 지역기반 contentid에도 존재하는 것만 남김
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
                            pickDistance(distanceByContentId, i), // 거리 선택 로직
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

    // 필요 시 특정 contentid 제거(정리 용도)
    public void remove(String contentId) {
        store.remove(contentId);
    }

    // 내부: 거리 선택(우선순위: 외부 제공 맵 → item.dist 파싱 → null)
    private static Double pickDistance(Map<String, Double> distanceByContentId, TourPoiResponse.Item i) {
        if (distanceByContentId != null) {
            Double v = distanceByContentId.get(i.contentid());
            if (v != null) return v;
        }
        return parseDist(i.dist());
    }

    // 내부: TourAPI item.dist 안전 파싱(문자열/숫자 모두 수용)
    private static Double parseDist(Object distField) {
        if (distField == null) return null;
        if (distField instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(distField.toString()); }
        catch (Exception e) { return null; }
    }

    public record List3Item(
            String contentId,
            String contentTypeId,
            String title,
            String addr1,
            String firstImage,
            Double mapX,
            Double mapY,
            Double distanceMeters, // 위치기반에서 계산 또는 item.dist 파싱
            String cat1,
            String cat2,
            String cat3
    ) {}
}
