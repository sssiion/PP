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
}
