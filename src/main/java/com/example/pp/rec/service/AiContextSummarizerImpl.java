package com.example.pp.rec.service;


import com.example.pp.rec.model.model;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class AiContextSummarizerImpl implements model.ContextSummarizer {

    private final model.AiApiService aiApiService;
    private final model.PromptService promptService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiContextSummarizerImpl(model.AiApiService aiApiService, model.PromptService promptService) {
        this.aiApiService = aiApiService;
        this.promptService = promptService;
    }

    @Override
    public Mono<String> summarize(String previousSummary, List<String> chatHistory, int turnCount) {
        try {
            String promptName = (turnCount > 10) ? "summarize_keywords" : "summarize_default";
            String historyString = String.join("\n", chatHistory);
            Map<String, Object> data = Map.of(
                    "previousSummary", previousSummary,
                    "history", historyString
            );

            String fullPrompt = promptService.buildPrompt(promptName, data);

            return aiApiService.getAiResponse(fullPrompt) // 1. 비동기 호출
                    .map(aiResponseJson -> { // 2. .map으로 파싱
                        try {
                            if(aiResponseJson.contains("\"error\"")) return previousSummary;
                            Map<String, String> responseMap = objectMapper.readValue(aiResponseJson, Map.class);
                            return responseMap.getOrDefault("summary", previousSummary);
                        } catch (Exception e) {
                            return previousSummary; // 파싱 실패 시 이전 요약본
                        }
                    })
                    .onErrorReturn(previousSummary); // API 호출 실패 시

        } catch (Exception e) {
            e.printStackTrace();
            return Mono.just(previousSummary); // (동기) 프롬프트 빌드 실패 시
        }
    }
}