package com.example.pp.service;

import com.example.pp.config.TourApiV2Client;
import com.example.pp.dto.TourPoiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class List2SeoulAreaService {

    private static final Logger log = LoggerFactory.getLogger(List2SeoulAreaService.class);

    private final TourApiV2Client tour;

    // contentid 저장소(순서 보존 + 중복 제거)
    private final Set<String> store = Collections.synchronizedSet(new LinkedHashSet<>());

    public Mono<List<String>> buildAndStore(int pageSize) {
        // pageSize는 10000 등 큰 값으로 호출 측에서 보장한다고 가정
        return tour.areaBasedList2Seoul() // 내부에서 pageNo=1, numOfRows=10000로 호출
                .map(first -> {
                    List<String> ids = extractContentIds(first);
                    store.clear();
                    store.addAll(ids);
                    log.info("[List2] 단일 페이지 수집 완료 saved={}, requestedPageSize={}", store.size(), pageSize);
                    return new ArrayList<>(store);
                });
    }
    // 저장소 가져오기(리스트3 비교용으로 전달)
    public List<String> getSavedContentIds() {
        return new ArrayList<>(store);
    }

    // 저장소 비우기
    public void clear() {
        store.clear();
    }

    // 저장소 크기
    public int size() {
        return store.size();
    }

    // 응답에서 contentid 리스트만 추출
    private List<String> extractContentIds(TourPoiResponse resp) {
        return Optional.ofNullable(resp)
                .map(TourPoiResponse::response)
                .map(TourPoiResponse.Resp::body)
                .map(TourPoiResponse.Body::items)
                .map(TourPoiResponse.Items::item)
                .orElseGet(List::of)
                .stream()
                .map(TourPoiResponse.Item::contentid)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
