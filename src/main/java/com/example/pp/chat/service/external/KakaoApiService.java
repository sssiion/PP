package com.example.pp.chat.service.external;


import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class KakaoApiService {
    private final WebClient kakaoWebClient;

    public Mono<List<Map<String,Object>>> geocodeByAddress(String address){
        return kakaoWebClient.get()
                .uri(uri -> uri.path("/v2/local/search/address.json").queryParam("query", address).build())
                .retrieve().bodyToMono(Map.class)
                .map(m -> (List<Map<String,Object>>) m.getOrDefault("documents", List.of()));
    }

    public Mono<List<Map<String,Object>>> searchByKeyword(String query, double lat, double lon, int size, int radius){
        return kakaoWebClient.get()
                .uri(uri -> uri.path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .queryParam("y", String.valueOf(lat)).queryParam("x", String.valueOf(lon))
                        .queryParam("radius", Math.min(radius,20000)).queryParam("size", Math.min(size,15))
                        .queryParam("sort","distance").build())
                .retrieve().bodyToMono(Map.class)
                .map(m -> (List<Map<String,Object>>) m.getOrDefault("documents", List.of()));
    }
}