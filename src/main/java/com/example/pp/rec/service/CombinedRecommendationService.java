package com.example.pp.rec.service;

import com.example.pp.rec.dto.CongestionRequestDto;
import com.example.pp.rec.dto.CongestionResponseDto;
import com.example.pp.rec.service.list1.list1_orchestrator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CombinedRecommendationService {

    private final list1_orchestrator list1Service;
    private final CongestionService congestionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<List<Map<String, Object>>> recommendWithCongestion(
            Double lat, Double lon, LocalTime time, Integer radius, Integer pageSize, List<String> categories, String congestionDateTime) {

        return list1Service.build(lat, lon, time, radius, pageSize, categories)
                .flatMap(recommendations -> {
                    if (recommendations == null || recommendations.isEmpty()) {
                        return Mono.just(new ArrayList<>());
                    }

                    // 1. 추천 결과(객체 리스트)를 순회하며 혼잡도 요청 DTO 생성
                    List<CongestionRequestDto> congestionRequests = new ArrayList<>();
                    for (Map<String, Object> seedGroup : recommendations) {
                        Map<String, List<?>> results = (Map<String, List<?>>) seedGroup.get("results");
                        if (results == null) continue;

                        for (List<?> places : results.values()) {
                            for (Object placeObj : places) {
                                Map<String, Object> placeMap = objectMapper.convertValue(placeObj, new TypeReference<>() {});
                                Object placeLat = placeMap.get("latitude");
                                Object placeLon = placeMap.get("longitude");
                                if (placeLat != null && placeLon != null) {
                                    congestionRequests.add(new CongestionRequestDto(
                                            placeLat.toString(),
                                            placeLon.toString(),
                                            congestionDateTime
                                    ));
                                }
                            }
                        }
                    }

                    // 2. 혼잡도 API 호출
                    return congestionService.getCongestion(congestionRequests)
                            .map(congestionResponse -> {
                                // 3. 빠른 조회를 위해 혼잡도 결과를 Map으로 변환
                                Map<String, String> congestionMap = congestionResponse.stream()
                                        .collect(Collectors.toMap(
                                                res -> res.getLatitude() + "," + res.getLongitude(),
                                                CongestionResponseDto::getCongestionLevel,
                                                (level1, level2) -> level1
                                        ));

                                // 4. 최종 결과를 담을 새로운 List 생성
                                List<Map<String, Object>> newRecommendations = new ArrayList<>();

                                // 5. 기존 추천 결과를 순회하며 새로운 데이터 구조 조립
                                for (Map<String, Object> seedGroup : recommendations) {
                                    Map<String, Object> newSeedGroup = new LinkedHashMap<>(seedGroup);
                                    Map<String, List<?>> results = (Map<String, List<?>>) newSeedGroup.get("results");
                                    if (results == null) {
                                        newRecommendations.add(newSeedGroup);
                                        continue;
                                    }

                                    Map<String, List<Map<String, Object>>> newResults = new LinkedHashMap<>();
                                    for (Map.Entry<String, List<?>> entry : results.entrySet()) {
                                        String category = entry.getKey();
                                        List<?> places = entry.getValue();

                                        List<Map<String, Object>> newPlaces = new ArrayList<>();
                                        for (Object placeObj : places) {
                                            // 장소 객체를 Map으로 변환
                                            Map<String, Object> placeMap = objectMapper.convertValue(placeObj, new TypeReference<>() {});

                                            // 혼잡도 정보 찾아서 추가
                                            Object placeLat = placeMap.get("latitude");
                                            Object placeLon = placeMap.get("longitude");
                                            String key = (placeLat != null && placeLon != null) ? placeLat.toString() + "," + placeLon.toString() : null;
                                            String congestionLevel = (key != null) ? congestionMap.getOrDefault(key, "") : "";
                                            placeMap.put("congestionLevel", congestionLevel);

                                            newPlaces.add(placeMap);
                                        }
                                        newResults.put(category, newPlaces);
                                    }
                                    newSeedGroup.put("results", newResults);
                                    newRecommendations.add(newSeedGroup);
                                }
                                return newRecommendations;
                            });
                });
    }
}
