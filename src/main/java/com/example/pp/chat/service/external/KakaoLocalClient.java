package com.example.pp.chat.service.external;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class KakaoLocalClient {
    private final WebClient kakaoWebClient;
    private static final ParameterizedTypeReference<Map<String,Object>> MAP_REF =
            new ParameterizedTypeReference<Map<String,Object>>() {};

    public KakaoLocalClient(WebClient kakaoWebClient) {
        this.kakaoWebClient = kakaoWebClient;
    }

    private <T> Mono<T> retrieveWithBody(WebClient.RequestHeadersSpec<?> spec, Class<T> clazz) {
        return spec.retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new KakaoApiException(resp.statusCode().value(), body)))
                )
                .bodyToMono(clazz);
    }

    private Mono<Map<String,Object>> retrieveWithBodyMap(WebClient.RequestHeadersSpec<?> spec) {
        return spec.retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new KakaoApiException(resp.statusCode().value(), body)))
                )
                .bodyToMono(MAP_REF);
    }

    public Mono<List<Map<String,Object>>> searchKeywordPaged(String query, double lat, double lon, int radiusMeters) {
        WebClient.RequestHeadersSpec<?> req1 = kakaoWebClient.get().uri(uri -> uri.path("/v2/local/search/keyword.json")
                .queryParam("query", query)
                .queryParam("x", String.valueOf(lon))
                .queryParam("y", String.valueOf(lat))
                .queryParam("radius", Math.min(radiusMeters, 20000))
                .queryParam("size", 15)
                .queryParam("page", 1)
                .queryParam("sort", "distance")
                .build());

        WebClient.RequestHeadersSpec<?> req2 = kakaoWebClient.get().uri(uri -> uri.path("/v2/local/search/keyword.json")
                .queryParam("query", query)
                .queryParam("x", String.valueOf(lon))
                .queryParam("y", String.valueOf(lat))
                .queryParam("radius", Math.min(radiusMeters, 20000))
                .queryParam("size", 15)
                .queryParam("page", 2)
                .queryParam("sort", "distance")
                .build());

        Mono<Map<String,Object>> p1 = retrieveWithBodyMap(req1);
        Mono<Map<String,Object>> p2 = retrieveWithBodyMap(req2);

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
}
