package com.example.pp.chat.service.external;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoLocalClient {
    private final WebClient kakaoWebClient; // baseUrl=https://dapi.kakao.com, default header Authorization: KakaoAK {REST_API_KEY}

    public Mono<List<Map<String,Object>>> searchKeywordPaged(String query, double lat, double lon, int radiusMeters) {
        ParameterizedTypeReference<Map<String,Object>> MAP_REF = new ParameterizedTypeReference<Map<String,Object>>() {};

        Mono<Map<String,Object>> p1 = kakaoWebClient.get()
                .uri(uri -> uri.path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .queryParam("x", String.valueOf(lon))
                        .queryParam("y", String.valueOf(lat))
                        .queryParam("radius", Math.min(radiusMeters, 20000))
                        .queryParam("size", 15)
                        .queryParam("page", 1)
                        .queryParam("sort", "distance")
                        .build())
                .retrieve()
                .bodyToMono(MAP_REF)
                .onErrorResume(e -> Mono.just(emptyKakaoResponse()));

        Mono<Map<String,Object>> p2 = kakaoWebClient.get()
                .uri(uri -> uri.path("/v2/local/search/keyword.json")
                        .queryParam("query", query)
                        .queryParam("x", String.valueOf(lon))
                        .queryParam("y", String.valueOf(lat))
                        .queryParam("radius", Math.min(radiusMeters, 20000))
                        .queryParam("size", 15)
                        .queryParam("page", 2)
                        .queryParam("sort", "distance")
                        .build())
                .retrieve()
                .bodyToMono(MAP_REF)
                .onErrorResume(e -> Mono.just(emptyKakaoResponse()));

        return Mono.zip(p1, p2)
                .map(tuple -> {
                    List<Map<String,Object>> d1 = castDocs(tuple.getT1().get("documents"));
                    List<Map<String,Object>> d2 = castDocs(tuple.getT2().get("documents"));
                    Map<String, Map<String,Object>> byId = new LinkedHashMap<>();
                    for (Map<String,Object> m : d1) byId.put(String.valueOf(m.get("id")), m);
                    for (Map<String,Object> m : d2) byId.put(String.valueOf(m.get("id")), m);
                    return new ArrayList<>(byId.values());
                });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> castDocs(Object docs) {
        return (docs instanceof List) ? (List<Map<String,Object>>) docs : List.of();
    }

    private Map<String,Object> emptyKakaoResponse() {
        return Map.of("documents", List.of(), "meta", Map.of());
    }


    public Mono<List<Map<String,Object>>> geocodeAddress(String address) {
        return kakaoWebClient.get()
                .uri(uri -> uri.path("/v2/local/search/address.json")
                        .queryParam("query", address)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                .map(res -> (List<Map<String,Object>>) res.getOrDefault("documents", List.of()))
                .onErrorResume(e -> Mono.just(List.of()));
    }
}
