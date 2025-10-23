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

import java.util.List;
import java.util.Map;

/**
 * AiApiService(설계도)를 실제로 구현하는 '엔진' 클래스입니다.
 * Google Gemini API 연동을 전담합니다.
 */
@Service
public class AiApiServiceImpl implements model.AiApiService { // <--- AiApiService를 'implements'

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final model.PromptService promptService; // 헬퍼 메서드를 위해 주입

    @Value("${google.ai.apikey}")
    private String apiKey;

    @Value("${google.ai.api.url}") // 예: "https://generativelanguage.googleapis.com/..."
    private String apiUrl;

    // 생성자: PromptService도 함께 주입받도록 수정
    public AiApiServiceImpl(WebClient.Builder webClientBuilder, model.PromptService promptService) {
        this.webClient = webClientBuilder.build();
        this.promptService = promptService;
    }

    /**
     * [메인 기능 구현]
     * Gemini API를 직접 호출하는 핵심 메서드
     */
    @Override
    public String getAiResponse(String fullPrompt) {
        try {
            // 1. Google AI 요청 본문 생성
            Map<String, List<Map<String, List<Map<String, String>>>>> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", fullPrompt)
                            ))
                    )
            );

            // 2. WebClient로 API 호출
            String rawResponse = webClient.post()
                    .uri(apiUrl, uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // 동기식 대기

            // 3. AI의 응답 JSON에서 실제 텍스트 내용물 파싱
            return parseAiResponseContent(rawResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"AI 호출에 실패했습니다.\", \"reply\": \"AI 응답 생성 중 오류가 발생했습니다.\"}";
        }
    }

    /**
     * Gemini API의 응답 JSON에서 AI가 생성한 텍스트(JSON)를 추출하는 헬퍼
     */
    private String parseAiResponseContent(String rawGeminiResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawGeminiResponse);

        // (오류 처리)
        if (root.has("promptFeedback")) {
            System.err.println("AI 응답 거부됨: " + root.path("promptFeedback").toString());
            return "{\"error\": \"AI가 응답을 거부했습니다.\", \"reply\": \"AI가 응답을 거부했습니다.\"}";
        }

        // AI가 생성한 실제 텍스트(우리가 요청한 JSON)를 추출
        String aiText = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        if (aiText.isEmpty()) {
            return "{\"error\": \"AI 응답이 비어있습니다.\", \"reply\": \"AI가 빈 응답을 반환했습니다.\"}";
        }
        return aiText;
    }


    // --- [헬퍼 메서드 3개 실제 구현] ---

    /**
     * [헬퍼 1 구현] 사용자 쿼리 파싱 (txt: phase2_parse_query.txt)
     */
    @Override
    public ParsedPlaceQuery parseQuery(String summary, String userInput) {
        try {
            Map<String, Object> data = Map.of(
                    "summary", summary,
                    "userInput", userInput,
                    "previousData", "{}" // (필요시 context에서 가져온 데이터 주입)
            );
            String prompt = promptService.buildPrompt("phase2_parse_query", data);
            String aiResponseJson = getAiResponse(prompt); // 자기 자신의 메인 메서드 호출
            return objectMapper.readValue(aiResponseJson, ParsedPlaceQuery.class);
        } catch (Exception e) {
            System.err.println("AI 쿼리 파싱 실패: " + e.getMessage());
            return new ParsedPlaceQuery(); // 오류 시 빈 객체 반환
        }
    }

    /**
     * [헬퍼 2 구현] Naver 지도 JSON 파싱 (txt: phase2_parse_map_data.txt)
     */
    @Override
    public List<Place> parsePlacesFromMapData(String mapJson) {
        try {
            Map<String, Object> data = Map.of("mapJson", mapJson);
            String prompt = promptService.buildPrompt("phase2_parse_map_data", data);
            String aiResponseJson = getAiResponse(prompt);

            // AI가 JSON "배열"을 반환하므로 TypeReference 필요
            return objectMapper.readValue(aiResponseJson, new TypeReference<List<Place>>() {});
        } catch (Exception e) {
            System.err.println("AI 지도 파싱 실패: " + e.getMessage());
            return List.of(); // 오류 시 빈 리스트 반환
        }
    }

    /**
     * [헬퍼 3 구현] 블로그 리뷰 요약 (txt: summarize_external_data.txt)
     */
    @Override
    public String summarizeReviewSnippets(List<String> snippets) {
        try {
            if (snippets == null || snippets.isEmpty()) {
                return "관련 리뷰 정보를 찾을 수 없습니다.";
            }
            String reviewText = String.join("\n- ", snippets);
            Map<String, Object> data = Map.of("reviewText", reviewText);
            String prompt = promptService.buildPrompt("summarize_external_data", data);

            String aiResponseJson = getAiResponse(prompt);

            // AI가 {"summary": "..."}를 반환
            JsonNode root = objectMapper.readTree(aiResponseJson);
            return root.path("summary").asText("리뷰 요약에 실패했습니다.");
        } catch (Exception e) {
            System.err.println("AI 리뷰 요약 실패: " + e.getMessage());
            return "리뷰 요약 중 오류 발생";
        }
    }
}