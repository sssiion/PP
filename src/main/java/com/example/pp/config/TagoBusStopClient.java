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
@RequiredArgsConstructor
public class TagoBusStopClient {

    private final @Qualifier("tagoBusStopWebClient") WebClient tagoBusStopWebClient;
    @Value("${tago.api.service-key}") private String serviceKey;

    private static final ParameterizedTypeReference<OpenApiWrap<StopItem>> STOP_TYPE =
            new ParameterizedTypeReference<>() {};

    public Mono<OpenApiWrap<StopItem>> nearbyStops(double lon, double lat, int radiusMeters, int numOfRows) {
        final String 서비스명 = "국토교통부_(TAGO)_버스정류소정보"; // 실제 경로로 교체(예: /1613000/BusSttnInfoInqireService)
        return tagoBusStopWebClient.get().uri(uri -> uri
                        .path("/" + 서비스명 + "/getCrdntPrxmtSttnList")
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
