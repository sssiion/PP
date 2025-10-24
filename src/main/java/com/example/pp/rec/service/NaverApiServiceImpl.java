package com.example.pp.rec.service;

import com.example.pp.rec.model.NaverApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class NaverApiServiceImpl implements NaverApiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper(); // 블로그 파싱용

    @Value("${naver.client.id}")
    private String NAVER_CLIENT_ID;
    @Value("${naver.client.secret}")
    private String NAVER_CLIENT_SECRET;

    // --- (수정 1: 생성자) ---
    public NaverApiServiceImpl(WebClient.Builder webClientBuilder) {
        // 기본 URL을 공통 주소인 "https://openapi.naver.com"로 변경합니다.
        this.webClient = webClientBuilder.baseUrl("https://openapi.naver.com").build();
    }

    /**
     * [수정] Naver 지역 검색 (비동기)
     * .block() 제거, Mono<String> 반환
     */
    @Override
    public Mono<String> searchLocal(String query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/local.json") // <--- (수정) .json 추가
                        .queryParam("query", query)
                        .queryParam("display", 10)
                        .build())
                .header("X-Naver-Client-Id", NAVER_CLIENT_ID)
                .header("X-Naver-Client-Secret", NAVER_CLIENT_SECRET)
                .retrieve()
                .bodyToMono(String.class) // .block() 삭제!
                .onErrorReturn(""); // API 호출 실패 시 빈 문자열 반환
    }

    /**
     * [수정] Naver 블로그 검색 (비동기)
     * .block() 제거, Mono<List<String>> 반환, .map()으로 파싱
     */
    @Override
    public Mono<List<String>> searchBlogSnippets(String query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/blog.json") // <--- (수정) .json 추가
                        .queryParam("query", query + " 후기")
                        .queryParam("display", 5)
                        .build())
                .header("X-Naver-Client-Id", NAVER_CLIENT_ID)
                .header("X-Naver-Client-Secret", NAVER_CLIENT_SECRET)
                .retrieve()
                .bodyToMono(String.class) // .block() 삭제!
                .map(responseJson -> { // [신규] .map()을 이용해 비동기 체인 안에서 파싱
                    List<String> snippets = new ArrayList<>();
                    try {
                        JsonNode items = objectMapper.readTree(responseJson).path("items");
                        if (items.isArray()) {
                            for (JsonNode item : items) {
                                String description = item.path("description").asText()
                                        .replaceAll("<[^>]*>", ""); // HTML 태그 제거
                                snippets.add(description);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace(); // 파싱 오류 시
                    }
                    return snippets;
                })
                .onErrorReturn(Collections.emptyList()); // API 호출/파싱 실패 시 빈 리스트 반환
    }
}