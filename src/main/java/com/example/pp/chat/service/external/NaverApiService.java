package com.example.pp.chat.service.external;

import com.example.pp.chat.dto.BlogReview;
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

    // 지역 검색: 네이버 검색 API (지역/장소)
    public Mono<List<Map<String,Object>>> searchLocal(String query, Integer display, Integer start, String sort){
        return naverWebClient.get()
                .uri(uri -> uri.path("/v1/search/local.json")
                        .queryParam("query", query)
                        .queryParam("display", display!=null?display:10)
                        .queryParam("start", start!=null?start:1)
                        .queryParam("sort", sort!=null?sort:"random")
                        .build())
                .retrieve().bodyToMono(Map.class)
                .map(m -> (List<Map<String,Object>>) m.getOrDefault("items", List.of()));
    }

    // 블로그 검색: 네이버 블로그 API
    public Mono<List<String>> searchBlogSnippets(String query, Integer display, Integer start, String sort){
        return naverWebClient.get()
                .uri(uri -> uri.path("/v1/search/blog.json")
                        .queryParam("query", query)
                        .queryParam("display", display!=null?display:5)
                        .queryParam("start", start!=null?start:1)
                        .queryParam("sort", sort!=null?sort:"sim")
                        .build())
                .retrieve().bodyToMono(Map.class)
                .map(m -> {
                    List<Map<String,Object>> items = (List<Map<String,Object>>) m.getOrDefault("items", List.of());
                    List<String> desc = new ArrayList<>();
                    for (Map<String,Object> it: items){
                        Object d = it.get("description");
                        if (d!=null) desc.add(String.valueOf(d));
                    }
                    return desc;
                });
    }
    public Mono<List<BlogReview>> searchBlogReviewsAsParsed(String query, Integer display, Integer start, String sort){
        // JSON/REST 예시 (XML은 파서로 변환)
        return naverWebClient.get()
                .uri(uri -> uri.path("/v1/search/blog.json")
                        .queryParam("query", query)
                        .queryParam("display", display!=null?display:5)
                        .queryParam("start", start!=null?start:1)
                        .queryParam("sort", sort!=null?sort:"sim")
                        .build())
                .retrieve().bodyToMono(Map.class)
                .map(m -> {
                    List<Map<String,Object>> items = (List<Map<String,Object>>) m.getOrDefault("items", List.of());
                    List<BlogReview> reviews = new ArrayList<>();
                    for (Map<String,Object> it: items){
                        BlogReview r = new BlogReview();
                        r.setTitle(String.valueOf(it.get("title")));
                        r.setDescription(String.valueOf(it.get("description")));
                        r.setLink(String.valueOf(it.get("link")));
                        r.setBloggerName(String.valueOf(it.get("bloggername")));
                        r.setBloggerLink(String.valueOf(it.get("bloggerlink")));
                        r.setPostdate(String.valueOf(it.get("postdate")));
                        reviews.add(r);
                    }
                    return reviews;
                });
    }

}
