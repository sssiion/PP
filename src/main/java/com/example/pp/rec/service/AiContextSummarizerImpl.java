package com.example.pp.rec.service;

import com.example.pp.rec.model.model;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public String summarize(String previousSummary, List<String> chatHistory, int turnCount) {
        try {
            String promptName = (turnCount > 10) ? "summarize_keywords" : "summarize_default";
            String historyString = String.join("\n", chatHistory);
            Map<String, Object> data = Map.of(
                    "previousSummary", previousSummary,
                    "history", historyString
            );

            String fullPrompt = promptService.buildPrompt(promptName, data);
            String aiResponseJson = aiApiService.getAiResponse(fullPrompt);

            // 요약 프롬프트는 {"summary": "..."} 형식을 반환하도록 지시
            Map<String, String> responseMap = objectMapper.readValue(aiResponseJson, Map.class);
            return responseMap.getOrDefault("summary", previousSummary);

        } catch (Exception e) {
            e.printStackTrace();
            return previousSummary; // 실패 시 이전 요약본 유지
        }
    }
}