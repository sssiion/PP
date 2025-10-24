package com.example.pp.rec.model;

import com.example.pp.rec.dto.ParsedPlaceQuery;
import com.example.pp.rec.dto.Place;
import com.example.pp.rec.dto.ServerMessageResponse;
import com.example.pp.rec.entity.ChatContext;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class model {
    // 1. 자동화 지휘자
    public interface ChatOrchestrationService {
        Mono<ServerMessageResponse> processMessage(ChatContext context, String userMessage);
    }

    // 2. AI API 연동
    public interface AiApiService {
        // String -> Mono<String>
        Mono<String> getAiResponse(String fullPrompt);

        // ParsedPlaceQuery -> Mono<ParsedPlaceQuery>
        Mono<ParsedPlaceQuery> parseQuery(String summary, String userInput);

        // List<Place> -> Mono<List<Place>>
        Mono<List<Place>> parsePlacesFromMapData(String mapJson);

        // String -> Mono<String>
        Mono<String> summarizeReviewSnippets(List<String> snippets);
    }

    // 3. 프롬프트(명령서) 관리
    public interface PromptService {
        String buildPrompt(String promptName, Map<String, Object> data);
    }

    // 4. 대화 요약
    public interface ContextSummarizer {
        Mono<String> summarize(String previousSummary, List<String> chatHistory, int turnCount);
    }

    // 5. DB 데이터 관리
    public interface PlaceDataService {
        List<Place> getPlacesByLocation(String location, double radius);
        List<Place> filterByCategory(List<Place> places, String category);
    }





}

