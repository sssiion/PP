package com.example.pp.rec.service;

import com.example.pp.rec.dto.CongestionRequestDto;
import com.example.pp.rec.dto.RouteResponseDto;
import com.example.pp.rec.dto.TmapRouteResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class RouteRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RouteRecommendationService.class);
    private final TmapService tmapService;
    private final CongestionService congestionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<RouteResponseDto> getRouteWithCongestion(double startX, double startY, double endX, double endY, String sort, String departureTime) {
        List<String> searchOptions = List.of("0", "10", "30");

        return Flux.fromIterable(searchOptions)
                .flatMap(option -> tmapService.getRoute(startX, startY, endX, endY, option)
                        .flatMap(tmapJson -> processSingleRouteResponse(tmapJson, departureTime))
                        .onErrorResume(e -> {
                            log.error("Error processing searchOption {}: {}", option, e.getMessage());
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

                    return new RouteResponseDto(uniqueRoutes);
                });
    }

    private Mono<RouteResponseDto.RecommendedRoute> processSingleRouteResponse(String tmapJson, String departureTime) {
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

                        double congestionScore = calculateCongestionScore(congestionPoints);

                        return new RouteResponseDto.RecommendedRoute(
                                totalTime,
                                totalDistance,
                                congestionScore,
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
