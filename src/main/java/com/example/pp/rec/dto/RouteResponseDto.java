package com.example.pp.rec.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteResponseDto {
    private List<RecommendedRoute> routes;
    private List<TransitRoute> transitRoutes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedRoute {
        private int durationInSeconds;
        private int distanceInMeters;
        private double congestionScore;
        private List<String> instructions; // Turn-by-turn instructions
        private Object routeGeometry; // The Tmap 'Feature' object for this route
        private List<CongestionPoint> congestionPoints;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CongestionPoint {
        private double latitude;
        private double longitude;
        private String congestionLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitRoute {
        private int totalTime;
        private int totalDistance;
        private int walkingDistance;
        private int fare;
        private List<TransitSegment> segments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitSegment {
        private String mode; // WALK, BUS, SUBWAY
        private String routeNumber; // e.g., "2호선" or "143"
        private String startName;
        private String endName;
        private double startX;
        private double startY;
        private double endX;
        private double endY;
        private int duration;
        private int distance;
        private String congestion; // e.g., "여유", "혼잡"
        private List<String> steps; // Detailed instructions for WALK mode
    }
}