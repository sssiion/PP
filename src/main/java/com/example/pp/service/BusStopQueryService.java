package com.example.pp.service;


import com.example.pp.config.TagoBusStopClient;
import com.example.pp.dto.BusStopDistanceDto;
import com.example.pp.dto.OpenApiWrap;
import com.example.pp.dto.StopItem;
import com.example.pp.util.Geo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusStopQueryService {
    private final TagoBusStopClient client;
    private static final int FIXED_RADIUS_M = 50;
    private static final int DEFAULT_ROWS = 200;

    public Mono<List<BusStopDistanceDto>> within50m(double lat, double lon) {
        return client.nearbyStops(lon, lat, FIXED_RADIUS_M, DEFAULT_ROWS)
                .map(this::toItems)
                .map(list -> list.stream()
                        .filter(s -> s.gpsLati() != null && s.gpsLong() != null)
                        .map(s -> toDistanceDto(s, lat, lon))
                        .sorted(Comparator.comparingDouble(BusStopDistanceDto::distanceMeters))
                        .collect(Collectors.toList()));
    }

    private List<StopItem> toItems(OpenApiWrap<StopItem> wrap) {
        return Optional.ofNullable(wrap)
                .map(OpenApiWrap::response)
                .map(OpenApiWrap.Response<StopItem>::body)
                .map(OpenApiWrap.Body<StopItem>::items)
                .map(OpenApiWrap.Items<StopItem>::item)
                .orElseGet(List::of);
    }

    private BusStopDistanceDto toDistanceDto(StopItem s, double originLat, double originLon) {
        double d = Geo.haversineMeters(originLat, originLon, s.gpsLati(), s.gpsLong());
        return new BusStopDistanceDto(
                s.cityCode(),
                s.nodeId(),
                s.nodeNm(),
                s.gpsLati(),
                s.gpsLong(),
                Math.round(d)
        );
    }
}
