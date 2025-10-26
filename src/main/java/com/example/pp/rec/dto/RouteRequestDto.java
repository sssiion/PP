package com.example.pp.rec.dto;

import lombok.Data;

@Data
public class RouteRequestDto {
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private String sort; // "congestion" or "duration"
    private String departureTime; // e.g., "2025-10-27T10:00:00"

}
