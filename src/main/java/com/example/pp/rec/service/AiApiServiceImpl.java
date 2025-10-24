package com.example.pp.rec.service;


import com.example.pp.rec.dto.ParsedPlaceQuery;
import com.example.pp.rec.dto.Place;

import com.example.pp.rec.model.model;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class AiApiServiceImpl implements model.AiApiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final model.PromptService promptService; // (model.model -> model로 수정)

    @Value("${google.ai.apikey}")
    private String apiKey;

    @Value("${google.ai.api.url}")
    private String apiUrl;

    public AiApiServiceImpl(WebClient.Builder webClientBuilder, model.PromptService promptService) {
        this.webClient = webClientBuilder.build();
        this.promptService = promptService;
    }

    @Override
    public Mono<String> getAiResponse(String fullPrompt) {
        try {
            Map<String, List<Map<String, List<Map<String, String>>>>> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", fullPrompt)
                            ))
                    )
            );

            return webClient.post()
                    .uri(apiUrl, uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(requestBody))
                    .retrieve()
                    .bodyToMono(String.class) // .block() 삭제!
                    .map(rawResponse -> { // map으로 체인 연결
                        try {
                            return parseAiResponseContent(rawResponse);
                        } catch (Exception e) {
                            throw new RuntimeException("AI 응답 파싱 실패", e);
                        }
                    })
                    .onErrorResume(e -> { // API/파싱 오류 시
                        e.printStackTrace();
                        return Mono.just("{\"error\": \"AI 호출에 실패했습니다.\", \"reply\": \"AI 응답 생성 중 오류가 발생했습니다.\"}");
                    });

        } catch (Exception e) { // 동기적 오류(JSON 생성 등)
            e.printStackTrace();
            return Mono.just("{\"error\": \"AI 요청 생성 실패\", \"reply\": \"AI 요청 생성 중 오류가 발생했습니다.\"}");
        }
    }

    private String parseAiResponseContent(String rawGeminiResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawGeminiResponse);

        if (root.has("promptFeedback")) {
            System.err.println("AI 응답 거부됨: " + root.path("promptFeedback").toString());
            return "{\"error\": \"AI가 응답을 거부했습니다.\", \"reply\": \"AI가 응답을 거부했습니다.\"}";
        }

        String aiText = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        if (aiText.isEmpty()) {
            return "{\"error\": \"AI 응답이 비어있습니다.\", \"reply\": \"AI가 빈 응답을 반환했습니다.\"}";
        }

        // --- [핵심 수정] Markdown 청소 ---
        // AI가 "\`\`\`json\n{...}\n\`\`\`" 처럼 응답하는 경우 대비
        if (aiText.startsWith("```")) {
            // "```json\n" 또는 "```\n" 부분을 제거
            int jsonStart = aiText.indexOf('{');
            if (jsonStart == -1) { // { 가 없으면
                jsonStart = aiText.indexOf('['); // [ (배열)인지 확인
            }

            // "```" 로 끝나는 부분 제거
            int jsonEnd = aiText.lastIndexOf('}');
            if (jsonEnd == -1) { // } 가 없으면
                jsonEnd = aiText.lastIndexOf(']'); // ] (배열)인지 확인
            }

            if (jsonStart != -1 && jsonEnd != -1) {
                aiText = aiText.substring(jsonStart, jsonEnd + 1);
            }
        }

        // 앞뒤 공백 제거
        return aiText.trim();
    }

    @Override
    public Mono<ParsedPlaceQuery> parseQuery(String summary, String userInput) {
        try {
            Map<String, Object> data = Map.of(
                    "summary", summary, "userInput", userInput, "previousData", "{}"
            );
            String prompt = promptService.buildPrompt("phase2_parse_query", data);

            return getAiResponse(prompt)
                    .map(aiResponseJson -> {
                        try {
                            // (방어 코드) 오류 JSON이 오면 빈 객체 반환
                            if(aiResponseJson.contains("\"error\"")) return new ParsedPlaceQuery();
                            return objectMapper.readValue(aiResponseJson, ParsedPlaceQuery.class);
                        } catch (Exception e) {
                            throw new RuntimeException("Query JSON 파싱 실패", e);
                        }
                    })
                    .onErrorReturn(new ParsedPlaceQuery()); // 전체 파이프라인 오류 시

        } catch (Exception e) {
            System.err.println("AI 쿼리 파싱 실패 (동기): " + e.getMessage());
            return Mono.just(new ParsedPlaceQuery());
        }
    }

    @Override
    public Mono<List<Place>> parsePlacesFromMapData(String mapJson) {
        try {
            Map<String, Object> data = Map.of("mapJson", mapJson);
            String prompt = promptService.buildPrompt("phase2_parse_map_data", data);

            return getAiResponse(prompt)
                    .map(aiResponseJson -> {
                        try {
                            if(aiResponseJson.contains("\"error\"")) {
                                // (수정) <Place> 타입 힌트 추가
                                return java.util.Collections.<Place>emptyList();
                            }
                            return objectMapper.readValue(aiResponseJson, new TypeReference<List<Place>>() {});
                        } catch (Exception e) {
                            throw new RuntimeException("Map JSON 파싱 실패", e);
                        }
                    })
                    // (수정) <Place> 타입 힌트 추가
                    .onErrorReturn(java.util.Collections.<Place>emptyList());

        } catch (Exception e) {
            System.err.println("AI 지도 파싱 실패 (동기): " + e.getMessage());
            // (수정) <Place> 타입 힌트 추가
            return Mono.just(java.util.Collections.<Place>emptyList());
        }
    }

    @Override
    public Mono<String> summarizeReviewSnippets(List<String> snippets) {
        try {
            if (snippets == null || snippets.isEmpty()) {
                return Mono.just("관련 리뷰 정보를 찾을 수 없습니다.");
            }
            String reviewText = String.join("\n- ", snippets);
            Map<String, Object> data = Map.of("reviewText", reviewText);
            String prompt = promptService.buildPrompt("summarize_external_data", data);

            return getAiResponse(prompt)
                    .map(aiResponseJson -> {
                        try {
                            if(aiResponseJson.contains("\"error\"")) return "리뷰 요약 실패";
                            JsonNode root = objectMapper.readTree(aiResponseJson);
                            return root.path("summary").asText("리뷰 요약에 실패했습니다.");
                        } catch (Exception e) {
                            throw new RuntimeException("Summary JSON 파싱 실패", e);
                        }
                    })
                    .onErrorReturn("리뷰 요약 중 오류 발생");

        } catch (Exception e) {
            System.err.println("AI 리뷰 요약 실패 (동기): " + e.getMessage());
            return Mono.just("리뷰 요약 중 오류 발생");
        }
    }
}