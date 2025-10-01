package com.example.pp.rec.service;



import com.example.pp.rec.dto.TourPoiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * List3 = List1(위치기반 TourAPI 아이템 원본) ∩ List2(지역기반 contentid 리스트)
 */
@Service
@RequiredArgsConstructor
public class List3BuildService {

    private static final Logger log = LoggerFactory.getLogger(List3BuildService.class);

    // 결과 캐시(필요 시 캐시/DB 교체)
    private final Map<String, List3Item> store = new LinkedHashMap<>();

    /**
     * @param list1Mono 위치기반 리스트1: TourAPI Item 원본 리스트
     * @param list2IdsMono 지역기반 리스트2: contentid 리스트(서울 전량)
     * @param distanceByContentId 선택: 외부에서 계산된 거리(m) 전달(없으면 item.dist 파싱 사용)
     */
    public Mono<List<List3Item>> buildAndStore(
            Mono<List<TourPoiResponse.Item>> list1Mono,
            Mono<List<String>> list2IdsMono,
            Map<String, Double> distanceByContentId
    ) {
        return Mono.zip(list1Mono, list2IdsMono).map(tuple -> {
            var list1 = Optional.ofNullable(tuple.getT1()).orElseGet(List::of);
            var list2Ids = new HashSet<>(Optional.ofNullable(tuple.getT2()).orElseGet(List::of));

            // contentid 교집합
            List<List3Item> list3 = list1.stream()
                    .filter(i -> i.contentid() != null && list2Ids.contains(i.contentid()))
                    .map(i -> new List3Item(
                            i.contentid(),
                            i.contenttypeid(),
                            i.title(),
                            i.addr1(),
                            i.firstimage(),
                            i.mapX(),
                            i.mapY(),
                            pickDistance(distanceByContentId, i),
                            i.cat1(),
                            i.cat2(),
                            i.cat3()
                    ))
                    // contentId 중복 방지
                    .collect(Collectors.toMap(List3Item::contentId, it -> it, (a,b)->a))
                    .values().stream().collect(Collectors.toList());

            // 저장
            list3.forEach(it -> store.put(it.contentId(), it));
            log.info("[List3] 교집합 완료 saved={}", store.size());
            return list3;
        });
    }

    // 저장 결과 조회/관리
    public List<List3Item> getAllSaved() { return new ArrayList<>(store.values()); }
    public void remove(String contentId) { store.remove(contentId); }
    public void clear() { store.clear(); }

    // 거리 선택 로직: 외부 맵 우선 → item.dist 파싱 → null
    private static Double pickDistance(Map<String, Double> distMap, TourPoiResponse.Item i) {
        if (distMap != null) {
            Double v = distMap.get(i.contentid());
            if (v != null) return v;
        }
        return parseDist(i.dist());
    }

    // TourAPI item.dist 안전 파싱
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
            Double distanceMeters,
            String cat1,
            String cat2,
            String cat3
    ) {}
}

