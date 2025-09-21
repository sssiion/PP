package com.example.pp.config;

import com.example.pp.dto.OpenApiWrap;
import com.example.pp.dto.StopItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component

public class TagoBusStopClient {

    private final WebClient tagoBusStopWebClient;
    private final String serviceKey;

    public TagoBusStopClient(
    @Qualifier("tagoBusStopWebClient") WebClient tagoBusStopWebClient,
    @Value("${tago.api.service-key}")  String serviceKey)
    {
        this.tagoBusStopWebClient = tagoBusStopWebClient;
        this.serviceKey = serviceKey;
    }

    private static final ParameterizedTypeReference<OpenApiWrap<StopItem>> STOP_TYPE =
            new ParameterizedTypeReference<>() {};

    public Mono<OpenApiWrap<StopItem>> nearbyStops(double lon, double lat, int radiusMeters, int numOfRows) {
        final String servicename = "1613000/BusSttnInfoInqireService"; // 실제 경로로 교체(예: /1613000/BusSttnInfoInqireService)
        return tagoBusStopWebClient.get().uri(uri -> uri
                        .path("/" + servicename + "/getCrdntPrxmtSttnList")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("_type", "json")
                        .queryParam("gpsLati", lat)
                        .queryParam("gpsLong", lon)
                        .queryParam("radius", radiusMeters)
                        .queryParam("numOfRows", numOfRows)
                        .build())
                .retrieve()
                .bodyToMono(STOP_TYPE);
    }
}
