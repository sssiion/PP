package com.example.pp.rec.service;

import com.example.pp.rec.model.NaverApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
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

    // --- (수정 2: searchLocal 경로) ---
    @Override
    public String searchLocal(String query) {
        // Naver 지역 검색 API 호출 (Raw JSON 반환)
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/local") // <--- '지역' 검색 경로로 수정
                        .queryParam("query", query)
                        .queryParam("display", 10) // 10개만
                        .build())
                .header("X-Naver-Client-Id", NAVER_CLIENT_ID)
                .header("X-Naver-Client-Secret", NAVER_CLIENT_SECRET)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // (실제로는 비동기 처리가 더 좋음)
    }

    // --- (수정 3: searchBlogSnippets 경로) ---
    @Override
    public List<String> searchBlogSnippets(String query) {
        // Naver 블로그 검색 API 호출
        String responseJson = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/blog") // <--- '블로그' 검색 경로로 수정
                        .queryParam("query", query + " 후기") // "후기" 키워드 추가
                        .queryParam("display", 5) // 5개만
                        .build())
                .header("X-Naver-Client-Id", NAVER_CLIENT_ID)
                .header("X-Naver-Client-Secret", NAVER_CLIENT_SECRET)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // JSON을 파싱해서 "description" 필드만 리스트로 반환
        List<String> snippets = new ArrayList<>();
        try {
            JsonNode items = objectMapper.readTree(responseJson).path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    // (HTML 태그 제거 추가)
                    String description = item.path("description").asText()
                            .replaceAll("<[^>]*>", ""); // <b>, </b> 태그 제거
                    snippets.add(description);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return snippets;
    }
}