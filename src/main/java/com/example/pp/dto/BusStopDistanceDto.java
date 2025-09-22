package com.example.pp.dto;

public record BusStopDistanceDto(
        String cityCode,
        String nodeId,
        String nodeNm,
        double lat,
        double lon,
        double distanceMeters
) {}
