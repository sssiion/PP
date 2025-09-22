package com.example.pp.config;

import com.example.pp.dto.OpenApiWrap;
import com.example.pp.dto.StopItem;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component

public class TagoBusStopClient {

    private final WebClient tagoBusStopWebClient;
    private final String serviceKey;
    private static final Logger log = LoggerFactory.getLogger(TagoBusStopClient.class);

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
        final String servicename = "/1613000/BusSttnInfoInqireService"; // 실제 경로로 교체(예: /1613000/BusSttnInfoInqireService)
        log.info("[TAGO 정류장 요청] 위도={}, 경도={}, 반경(m)={}, 최대건수={}", lat, lon, radiusMeters, numOfRows);
        return tagoBusStopWebClient.get().uri(uri -> uri
                        .path( servicename + "/getCrdntPrxmtSttnList")
                        .queryParam("serviceKey", "rUA%2FIQ7GuIQ3GK8uD%2F9BxWRIuMpc6g5%2B5ou8WDjh7UqRTnnG0cetRc3KXKV1E3kCaj5I6xEkKdJuqf09MJ0nWA%3D%3D")
                        .queryParam("_type", "json")
                        .queryParam("gpsLati", lat)
                        .queryParam("gpsLong", lon)
                        .queryParam("radius", radiusMeters)
                        .queryParam("numOfRows", numOfRows)
                        .build())
                .retrieve()
                .bodyToMono(STOP_TYPE)
                .doOnSubscribe(s -> log.info("[TAGO 정류장 진행] 호출 시작")) // 진행 로그 [web:77]
                .doOnError(e -> log.error("[TAGO 정류장 오류] {}", e.toString(), e)) // 오류 로그 [web:77]
                .doOnSuccess(w -> {
                    int count = Optional.ofNullable(w).map(OpenApiWrap::response)
                            .map(OpenApiWrap.Response<StopItem>::body)
                            .map(OpenApiWrap.Body<StopItem>::items)
                            .map(OpenApiWrap.Items<StopItem>::item)
                            .map(List::size).orElse(0);
                    log.info("[TAGO 정류장 응답] 수신 건수={}", count); // 결과 로그 [web:6]
                });
    }
}
