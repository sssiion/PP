package com.example.pp.rec.service;

import com.example.pp.rec.dto.CongestionRequestDto;
import com.example.pp.rec.dto.CongestionResponseDto;
import com.example.pp.rec.service.list1.list1_orchestrator;
import com.example.pp.rec.util.Geo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CombinedRecommendationService {

    private final list1_orchestrator list1Service;
    private final CongestionService congestionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 정렬 전략을 위한 Enum
    public enum SortBy {
        DISTANCE,
        CONGESTION
    }

    // 혼잡도 정렬 순서를 정의하는 맵
    private static final Map<String, Integer> CONGESTION_ORDER = Map.of(
            "여유", 1,
            "보통", 2,
            "약간 붐빔", 3,
            "붐빔", 4,
            "정보없음", 5
    );

    // Main public method that dispatches to the correct sorting strategy
    public Mono<List<Map<String, Object>>> recommendWithCongestion(
            Double lat, Double lon, LocalTime time, Integer radius, Integer pageSize, List<String> categories, LocalDateTime congestionDateTime, SortBy sortBy) {
        LocalTime effTime = (time != null) ? time : LocalTime.now();
        LocalDateTime effCdt = (congestionDateTime != null) ? congestionDateTime : LocalDateTime.now();


        return getEnrichedAndFlattenedPlaces(lat, lon, effTime, radius, pageSize, categories, effCdt)
                .map(places -> {
                    Comparator<Map<String, Object>> comparator = switch (sortBy) {
                        case CONGESTION -> Comparator
                                .comparing((Map<String, Object> place) -> CONGESTION_ORDER.getOrDefault(place.get("congestionLevel"), 5))
                                .thenComparing(place -> (Double) place.get("distance"));
                        case DISTANCE -> Comparator.comparing(place -> (Double) place.get("distance"));
                    };
                    return places.stream().sorted(comparator).collect(Collectors.toList());
                });
    }


    private Mono<List<Map<String, Object>>> getEnrichedAndFlattenedPlaces(
            Double lat, Double lon, LocalTime time, Integer radius, Integer pageSize, List<String> categories, LocalDateTime congestionDateTime) {
        LocalTime effTime = (time != null) ? time : LocalTime.now();
        LocalDateTime effCdt = (congestionDateTime != null) ? congestionDateTime : LocalDateTime.now();

        return list1Service.build(lat, lon, effTime, radius, pageSize, categories)
                .flatMap(recommendations -> {
                    if (recommendations == null || recommendations.isEmpty()) {
                        return Mono.just(new ArrayList<>());
                    }

                    List<Map<String, Object>> flattenedPlaces = new ArrayList<>();
                    Set<String> seenIds = new HashSet<>();
                    recommendations.forEach(seedGroup -> {
                        Map<String, List<?>> results = (Map<String, List<?>>) seedGroup.get("results");
                        if (results != null) {
                            results.values().forEach(places -> {
                                places.forEach(placeObj -> {
                                    Map<String, Object> placeMap = objectMapper.convertValue(placeObj, new TypeReference<>() {});
                                    String id = (String) placeMap.get("id");
                                    if (id != null && !seenIds.contains(id)) {
                                        flattenedPlaces.add(placeMap);
                                        seenIds.add(id);
                                    }
                                });
                            });
                        }
                    });

                    if (flattenedPlaces.isEmpty()) {
                        return Mono.just(new ArrayList<>());
                    }

                    List<CongestionRequestDto> congestionRequests = flattenedPlaces.stream()
                            .map(placeMap -> {
                                Object placeLat = placeMap.get("latitude");
                                Object placeLon = placeMap.get("longitude");
                                if (placeLat != null && placeLon != null) {
                                    return new CongestionRequestDto(
                                            Double.valueOf(placeLat.toString()),
                                            Double.valueOf(placeLon.toString()),
                                            effCdt.format(DateTimeFormatter.ISO_DATE_TIME)
                                    );
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    return congestionService.getCongestion(congestionRequests)
                            .map(congestionResponse -> {
                                Map<String, String> congestionMap = congestionResponse.stream()
                                        .collect(Collectors.toMap(
                                                res -> res.getLatitude() + "," + res.getLongitude(),
                                                CongestionResponseDto::getCongestionLevel,
                                                (level1, level2) -> level1
                                        ));

                                flattenedPlaces.forEach(placeMap -> {
                                    Object placeLatObj = placeMap.get("latitude");
                                    Object placeLonObj = placeMap.get("longitude");
                                    String congestionLevel = "정보없음";
                                    double distanceKm = -1.0;

                                    if (placeLatObj != null && placeLonObj != null) {
                                        String placeLatStr = placeLatObj.toString();
                                        String placeLonStr = placeLonObj.toString();
                                        String key = placeLatStr + "," + placeLonStr;
                                        congestionLevel = congestionMap.getOrDefault(key, "정보없음");

                                        try {
                                            double placeLat = Double.parseDouble(placeLatStr);
                                            double placeLon = Double.parseDouble(placeLonStr);
                                            double distanceMeters = Geo.haversineMeters(lat, lon, placeLat, placeLon);
                                            distanceKm = Math.round((distanceMeters / 1000.0) * 100.0) / 100.0; // km, 소수점 둘째 자리
                                        } catch (NumberFormatException e) {
                                            // 파싱 실패 시 거리는 -1.0으로 유지
                                        }
                                    }
                                    placeMap.put("congestionLevel", congestionLevel);
                                    placeMap.put("distance", distanceKm);
                                });

                                return flattenedPlaces;
                            });
                });
    }
}
