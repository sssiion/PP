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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NaverApiService {

    private final WebClient naverWebClient;
    private final RateLimiter naverLimiter;
    private final Retry externalRetry;

    @Value("${naver.client-id}")
    private String clientId;
    @Value("${naver.client-secret}")
    private String clientSecret;

    public Mono<List<Map<String,Object>>> searchLocal(String query, Integer display, Integer start, String sort) {
        return naverWebClient.get()
                .uri(uri -> uri.path("/v1/search/local.json")
                        .queryParam("query", query)
                        .queryParamIfPresent("display", Optional.ofNullable(display))
                        .queryParamIfPresent("start", Optional.ofNullable(start))
                        .queryParamIfPresent("sort", Optional.ofNullable(sort))
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(m -> (List<Map<String,Object>>) m.getOrDefault("items", List.of()));
    }

    public Mono<Map> searchBlog(String query, Integer display, Integer start, String sort) {
        return naverWebClient.get()
                .uri(uri -> uri.path("/v1/search/blog.json")
                        .queryParam("query", query)
                        .queryParamIfPresent("display", Optional.ofNullable(display))
                        .queryParamIfPresent("start", Optional.ofNullable(start))
                        .queryParamIfPresent("sort", Optional.ofNullable(sort))
                        .build())
                .retrieve()
                .bodyToMono(Map.class);
    }
    public Mono<List<String>> searchBlogSnippets(String query, Integer display, Integer start, String sort) {
        return searchBlog(query, display, start, sort)
                .map(m -> {
                    List<Map<String,Object>> items = (List<Map<String,Object>>) m.getOrDefault("items", List.of());
                    List<String> desc = new ArrayList<>();
                    for (Map<String,Object> it : items) {
                        Object d = it.get("description");
                        if (d != null) desc.add(String.valueOf(d));
                    }
                    return desc;
                });
    }
}
