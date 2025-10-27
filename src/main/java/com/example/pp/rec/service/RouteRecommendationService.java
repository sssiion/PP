package com.example.pp.rec.service;

import com.example.pp.chat.service.external.KakaoLocalClient;
import com.example.pp.rec.dto.CongestionRequestDto;
import com.example.pp.rec.dto.CongestionResponseDto;
import com.example.pp.rec.dto.PythonCongestionResponse;
import com.example.pp.rec.dto.RouteResponseDto;
import com.example.pp.rec.dto.TmapRouteResponseDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class RouteRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RouteRecommendationService.class);
    private final TmapService tmapService;
    private final CongestionService congestionService;
    private final KakaoLocalClient kakaoLocalClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouteRecommendationService(TmapService tmapService, CongestionService congestionService, KakaoLocalClient kakaoLocalClient) {
        this.tmapService = tmapService;
        this.congestionService = congestionService;
        this.kakaoLocalClient = kakaoLocalClient;
    }

    public Mono<RouteResponseDto> getRouteWithCongestion(double startX, double startY, double endX, double endY, String sort, String mode, String departureTime) {
        if ("transit".equalsIgnoreCase(mode)) {
            return tmapService.getTransitRoute(startX, startY, endX, endY)
                    .flatMap(tmapJson -> processTransitResponse(tmapJson, departureTime, startX, startY));
        }

        List<String> searchOptions = List.of("0", "10", "30");
        return Flux.fromIterable(searchOptions)
                .flatMap(option -> tmapService.getRoute(startX, startY, endX, endY, option)
                        .flatMap(tmapJson -> processWalkResponse(tmapJson, departureTime))
                        .onErrorResume(e -> {
                            log.error("Error processing walk searchOption {}: {}", option, e.getMessage());
                            return Mono.empty();
                        }))
                .collectList()
                .map(routes -> {
                    List<RouteResponseDto.RecommendedRoute> uniqueRoutes = routes.stream()
                            .filter(distinctByKey(r -> r.getDurationInSeconds() + ":" + r.getDistanceInMeters()))
                            .collect(Collectors.toList());

                    Comparator<RouteResponseDto.RecommendedRoute> comparator = "congestion".equalsIgnoreCase(sort)
                            ? Comparator.comparing(RouteResponseDto.RecommendedRoute::getCongestionScore)
                            .thenComparing(RouteResponseDto.RecommendedRoute::getDurationInSeconds)
                            : Comparator.comparing(RouteResponseDto.RecommendedRoute::getDurationInSeconds);
                    uniqueRoutes.sort(comparator);

                    return new RouteResponseDto(uniqueRoutes, null);
                });
    }

    private Mono<RouteResponseDto> processTransitResponse(String tmapJson, String departureTime, double startX, double startY) {
        try {
            Map<String, Object> tmapResponse = objectMapper.readValue(tmapJson, new TypeReference<>() {});
            Map<String, Object> metaData = (Map<String, Object>) tmapResponse.get("metaData");
            if (metaData == null || !metaData.containsKey("plan")) {
                return Mono.just(new RouteResponseDto(null, Collections.emptyList()));
            }

            Map<String, Object> plan = (Map<String, Object>) metaData.get("plan");
            List<Map<String, Object>> itineraries = (List<Map<String, Object>>) plan.get("itineraries");

            return Flux.fromIterable(itineraries)
                    .flatMap(itinerary -> {
                        List<Map<String, Object>> legs = (List<Map<String, Object>>) itinerary.get("legs");

                        return Flux.fromIterable(legs)
                                .concatMap(leg -> processTransitLeg(leg, departureTime, startX, startY))
                                .collectList()
                                .map(segments -> {
                                    Map<String, Object> fare = (Map<String, Object>) itinerary.get("fare");
                                    int totalFare = fare != null ? getInt(fare, "totalFare") : 0;

                                    return new RouteResponseDto.TransitRoute(
                                            getInt(itinerary, "totalTime"),
                                            getInt(itinerary, "totalDistance"),
                                            getInt(itinerary, "totalWalkDistance"),
                                            totalFare,
                                            segments
                                    );
                                });
                    })
                    .collectList()
                    .map(transitRoutes -> new RouteResponseDto(null, transitRoutes));

        } catch (Exception e) {
            log.error("Failed to process Tmap transit response", e);
            return Mono.error(e);
        }
    }

    private Mono<RouteResponseDto.TransitSegment> processTransitLeg(Map<String, Object> leg, String departureTime, double currentX, double currentY) {
        String mode = (String) leg.get("mode");
        Map<String, Object> start = (Map<String, Object>) leg.get("start");
        Map<String, Object> end = (Map<String, Object>) leg.get("end");
        String routeName = "WALK".equals(mode) ? "도보" : (String) leg.get("route");
        String startName = start != null ? (String) start.get("name") : "";

        List<String> steps = new ArrayList<>();
        if (leg.containsKey("steps")) {
            List<Map<String, Object>> stepsList = (List<Map<String, Object>>) leg.get("steps");
            for (Map<String, Object> step : stepsList) {
                if (step.containsKey("description")) {
                    steps.add((String) step.get("description"));
                }
            }
        }

        Mono<double[]> coordsMono;
        if (start != null && start.containsKey("lat") && start.containsKey("lon")) {
            coordsMono = Mono.just(new double[]{getDouble(start, "lat"), getDouble(start, "lon")});
        } else {
            coordsMono = kakaoLocalClient.searchKeywordPaged(startName, currentY, currentX, 20000)
                    .map(results -> {
                        if (results.isEmpty()) {
                            return new double[]{0.0, 0.0};
                        }
                        Map<String, Object> firstResult = results.get(0);
                        double lat = Double.parseDouble(firstResult.get("y").toString());
                        double lon = Double.parseDouble(firstResult.get("x").toString());
                        return new double[]{lat, lon};
                    });
        }

        return coordsMono.flatMap(coords -> {
            if (coords[0] == 0.0) {
                return Mono.just(new RouteResponseDto.TransitSegment(mode, routeName, startName, end != null ? (String) end.get("name") : "", getInt(leg, "sectionTime"), getInt(leg, "distance"), "정보없음", steps));
            }

            CongestionRequestDto request = new CongestionRequestDto(coords[0], coords[1], getCongestionTime(departureTime));
            Mono<String> congestionMono = congestionService.getCongestion(List.of(request))
                    .map(responses -> responses.isEmpty() ? "정보없음" : responses.get(0).getCongestionLevel())
                    .onErrorResume(e -> Mono.just("정보없음"));

            return congestionMono.map(congestion -> new RouteResponseDto.TransitSegment(
                    mode, routeName, startName, end != null ? (String) end.get("name") : "", getInt(leg, "sectionTime"), getInt(leg, "distance"), congestion, steps
            ));
        });
    }

    private String scoreToCongestionLevel(double score) {
        if (score < 2.0) return "여유";
        if (score < 4.0) return "보통";
        if (score < 6.0) return "약간 붐빔";
        if (score >= 6.0) return "붐빔";
        return "정보없음";
    }

    private double calculateCongestionScoreFromLevels(List<CongestionResponseDto> points) {
        if (points == null || points.isEmpty()) return 0.0;
        double totalScore = points.stream()
                .mapToDouble(point -> {
                    switch (point.getCongestionLevel()) {
                        case "여유": return 1.0;
                        case "보통": return 3.0;
                        case "약간 붐빔": return 5.0;
                        case "붐빔": return 8.0;
                        case "정보없음":
                        default: return 3.0;
                    }
                })
                .sum();
        return totalScore / points.size();
    }

    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private Mono<RouteResponseDto.RecommendedRoute> processWalkResponse(String tmapJson, String departureTime) {
        log.info("Tmap API Response: {}", tmapJson);
        try {
            if (tmapJson.contains("error")) {
                log.error("Tmap API returned an error: {}", tmapJson);
                return Mono.empty();
            }

            String cleanedTmapJson = tmapJson.replaceAll("\\u0000", "");
            TmapRouteResponseDto tmapResponse = objectMapper.readValue(cleanedTmapJson, TmapRouteResponseDto.class);
            if (tmapResponse.getFeatures() == null || tmapResponse.getFeatures().isEmpty()) {
                return Mono.empty();
            }

            TmapRouteResponseDto.Properties summaryProperties = tmapResponse.getFeatures().get(0).getProperties();
            int totalTime = summaryProperties != null && summaryProperties.getTotalTime() != null ? summaryProperties.getTotalTime() : 0;
            int totalDistance = summaryProperties != null && summaryProperties.getTotalDistance() != null ? summaryProperties.getTotalDistance() : 0;

            List<String> instructions = tmapResponse.getFeatures().stream()
                .filter(f -> "Point".equals(f.getGeometry().getType()) && f.getProperties() != null && f.getProperties().getDescription() != null)
                .map(f -> f.getProperties().getDescription())
                .collect(Collectors.toList());

            List<double[]> allCoordinates = new ArrayList<>();
            for (TmapRouteResponseDto.Feature feature : tmapResponse.getFeatures()) {
                if ("LineString".equals(feature.getGeometry().getType())) {
                    List<List<Double>> lineString = (List<List<Double>>) feature.getGeometry().getCoordinates();
                    for (List<Double> point : lineString) {
                        if (point.size() >= 2) allCoordinates.add(new double[]{point.get(0), point.get(1)});
                    }
                }
            }

            if (allCoordinates.isEmpty()) return Mono.empty();

            final String finalCongestionTime = getCongestionTime(departureTime);

            List<CongestionRequestDto> congestionRequests = allCoordinates.stream()
                    .map(coord -> new CongestionRequestDto(coord[1], coord[0], finalCongestionTime))
                    .collect(Collectors.toList());

            return congestionService.getCongestion(congestionRequests)
                    .map(congestionResponses -> {
                        List<RouteResponseDto.CongestionPoint> congestionPoints = congestionResponses.stream()
                                .map(res -> new RouteResponseDto.CongestionPoint(res.getLatitude(), res.getLongitude(), res.getCongestionLevel()))
                                .collect(Collectors.toList());

                        double congestionScore = calculateCongestionScoreFromLevels(congestionResponses);

                        return new RouteResponseDto.RecommendedRoute(
                                totalTime,
                                totalDistance,
                                congestionScore,
                                instructions,
                                tmapResponse,
                                congestionPoints
                        );
                    });

        } catch (Exception e) {
            log.error("Failed to process Tmap response", e);
            return Mono.error(e);
        }
    }

    private String getCongestionTime(String departureTime) {
        if (departureTime != null && !departureTime.isBlank()) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(departureTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e) {
                log.warn("Could not parse departureTime '{}', defaulting to now.", departureTime);
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } else {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }

    private double calculateCongestionScore(List<RouteResponseDto.CongestionPoint> points) {
        if (points == null || points.isEmpty()) return 0.0;
        double totalScore = points.stream()
                .mapToDouble(point -> {
                    switch (point.getCongestionLevel()) {
                        case "여유": return 1.0;
                        case "보통": return 3.0;
                        case "약간 붐빔": return 5.0;
                        case "붐빔": return 8.0;
                        case "정보없음":
                        default: return 3.0;
                    }
                })
                .sum();
        return totalScore / points.size();
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
