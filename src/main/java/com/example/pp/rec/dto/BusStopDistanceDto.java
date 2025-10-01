package com.example.pp.rec.dto;

public record BusStopDistanceDto(
        String cityCode,
        String nodeId,
        String nodeNm,
        double lat,
        double lon,
        double distanceMeters
) {}
