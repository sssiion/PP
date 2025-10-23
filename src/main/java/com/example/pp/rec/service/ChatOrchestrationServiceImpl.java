package com.example.pp.rec.service;


import com.example.pp.rec.dto.*;
import com.example.pp.rec.entity.ChatContext;
import com.example.pp.rec.model.NaverApiService;
import com.example.pp.rec.model.model;
import com.example.pp.rec.protocol.EnrichedPlace;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatOrchestrationServiceImpl implements ChatOrchestrationService {

    private final model.AiApiService aiApiService;
    private final model.PromptService promptService;
    private final model.ContextSummarizer contextSummarizer;
    private final PlaceDataService placeDataService;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 파싱용

    // [신규] NaverApiService 주입
    private final NaverApiService naverApiService;

    public ChatOrchestrationServiceImpl(model.AiApiService aiApiService,
                                        model.PromptService promptService,
                                        model.ContextSummarizer contextSummarizer,
                                        PlaceDataService placeDataService,
                                        NaverApiService naverApiService) {
        this.aiApiService = aiApiService;
        this.promptService = promptService;
        this.contextSummarizer = contextSummarizer;
        this.placeDataService = placeDataService;
        this.naverApiService = naverApiService; // 주입 추가
    }

    @Override
    public ServerMessageResponse processMessage(ChatContext context, String userMessage) {
        try {
            // --- 0. 맥락(Context) 준비 ---
            context.getFullHistory().add("User: " + userMessage);
            context.setTurnCount(context.getTurnCount() + 1);

            // AI에게 요약을 시킴 (상태 리셋 방지)
            String summary = contextSummarizer.summarize(
                    context.getSummarizedContext(),
                    context.getFullHistory(),
                    context.getTurnCount()
            );
            context.setSummarizedContext(summary); // 새 요약본으로 업데이트

            // --- 1. 상태(State)에 따른 로직 분기 ---
            ServerMessageResponse response;
            switch (context.getCurrentState()) {

                case INITIAL:
                case COLLECTING_DATA:
                    response = handleDataCollection(context, userMessage, summary);
                    break;

                case PLACE_QUERY_READY:
                case RECOMMENDING_PLACE:
                    context.setCurrentState(ChatState.RECOMMENDING_PLACE); // 상태 고정
                    response = handlePlaceRecommendation(context, userMessage, summary);
                    break;

                default:
                    response = new ServerMessageResponse("알 수 없는 오류가 발생했습니다.");
            }

            context.getFullHistory().add("AI: " + response.getReplyText());
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return new ServerMessageResponse("처리 중 심각한 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // --- 1단계 로직: 데이터 수집 ---
    private ServerMessageResponse handleDataCollection(ChatContext context, String userMessage, String summary) throws Exception {
        Map<String, Object> data = Map.of("summary", summary, "userInput", userMessage);
        String prompt = promptService.buildPrompt("phase1_collect_data", data);
        String aiResponseJson = aiApiService.getAiResponse(prompt);
        Phase1Response responseDto = objectMapper.readValue(aiResponseJson, Phase1Response.class);

        if ("PLACE_QUERY_READY".equalsIgnoreCase(responseDto.getStatus())) {
            context.setCurrentState(ChatState.PLACE_QUERY_READY);
            if (responseDto.getCollectedData() != null) {
                context.getCollectedData().putAll(responseDto.getCollectedData());
            }
        }
        return new ServerMessageResponse(responseDto.getReply());
    }

    // --- 2단계 로직: 장소 추천 (대폭 수정) ---
    private ServerMessageResponse handlePlaceRecommendation(ChatContext context, String userMessage, String summary) throws Exception {

        // --- 모듈 1: AI (사용자 의도 파싱) ---
        // (txt: phase2_parse_query.txt 사용)
        System.out.println("[Orchestrator] 모듈 1: 사용자 의도 파싱 시작...");
        ParsedPlaceQuery query = aiApiService.parseQuery(summary, userMessage);
        String searchQuery = query.getLocation() + " " + query.getCategory() + " " + query.getAtmosphere();

        // --- 모듈 2: 서버 (데이터 수집 및 강화) ---
        System.out.println("[Orchestrator] 모듈 2: 데이터 수집/강화 시작...");

        // 2a. [내부 DB]
        List<Place> dbPlaces = placeDataService.getPlacesByLocation(query.getLocation(), query.getRadius());
        List<Place> filteredDbPlaces = placeDataService.filterByCategory(dbPlaces, query.getCategory());

        // 2b. [외부 Naver Map API] + [AI 파싱]
        System.out.println("[Orchestrator] Naver 지도 검색: " + searchQuery);
        String naverMapJson = naverApiService.searchLocal(searchQuery);
        // (txt: phase2_parse_map_data.txt 사용)
        List<Place> parsedMapPlaces = aiApiService.parsePlacesFromMapData(naverMapJson);

        // 2c. [데이터 결합 및 중복 제거]
        List<Place> combinedList = combineAndDeduplicate(filteredDbPlaces, parsedMapPlaces);
        List<EnrichedPlace> enrichedList = new ArrayList<>();
        System.out.println("[Orchestrator] 총 " + combinedList.size() + "개의 후보 장소 확보.");

        // 2d. [데이터 강화: 블로그 요약]
        // (API 호출 제한을 위해 최대 5개 장소만 강화)
        int limit = Math.min(combinedList.size(), 5);
        for (Place place : combinedList.subList(0, limit)) {
            System.out.println("[Orchestrator] 블로그 리뷰 요약 중: " + place.getName());

            // 1. Naver 블로그 스니펫 수집
            List<String> snippets = naverApiService.searchBlogSnippets(place.getName() + " " + query.getCategory() + " 후기");

            // 2. (txt: summarize_external_data.txt 사용) AI에게 리뷰 요약 지시
            String aiSummary = aiApiService.summarizeReviewSnippets(snippets);

            // 3. 강화된 리스트에 추가
            enrichedList.add(new EnrichedPlace(place, aiSummary));
        }

        // --- 모듈 3: AI (최종 분석 및 추천) ---
        System.out.println("[Orchestrator] 모듈 3: AI 최종 추천 시작...");

        // 1. '강화된' 목록을 AI에게 전달하기 위해 JSON으로 변환
        String placeListJson = objectMapper.writeValueAsString(enrichedList);

        // 2. (txt: phase2_recommend_final.txt) 프롬프트 빌드
        Map<String, Object> recommendData = Map.of(
                "summary", summary,
                "userInput", userMessage,
                "placeListJson", placeListJson // AI 요약본이 포함된 리스트 전달
        );
        String recommendPrompt = promptService.buildPrompt("phase2_recommend_final", recommendData);

        // 3. AI 최종 추천 호출
        String finalResponseJson = aiApiService.getAiResponse(recommendPrompt);
        FinalRecommendationResponse finalRec = objectMapper.readValue(finalResponseJson, FinalRecommendationResponse.class);

        return new ServerMessageResponse(
                finalRec.getRecommendation(),
                finalRec.getPros(),
                finalRec.getCons()
        );
    }

    // (중복 제거 헬퍼 메서드 추가)
    private List<Place> combineAndDeduplicate(List<Place> list1, List<Place> list2) {
        // (list1과 list2를 합치고, 장소 이름/주소 기준으로 중복을 제거하는 로직 구현)
        List<Place> combined = new ArrayList<>(list1);
        combined.addAll(list2);

        // 이름 기준 중복 제거 (간단한 예시)
        return combined.stream()
                .filter(p -> p.getName() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Place::getName, p -> p, (p1, p2) -> p1),
                        Map::values
                ))
                .stream().collect(Collectors.toList());
    }

}