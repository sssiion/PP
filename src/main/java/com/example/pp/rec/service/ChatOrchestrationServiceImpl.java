package com.example.pp.rec.service;


import com.example.pp.rec.dto.*;
import com.example.pp.rec.entity.ChatContext;
import com.example.pp.rec.model.NaverApiService;
import com.example.pp.rec.model.model;
import com.example.pp.rec.protocol.EnrichedPlace;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers; // Schedulers 임포트

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatOrchestrationServiceImpl implements ChatOrchestrationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final model.AiApiService aiApiService;
    private final model.PromptService promptService;
    private final model.ContextSummarizer contextSummarizer;
    private final PlaceDataService placeDataService;
    private final NaverApiService naverApiService;

    // (생성자는 동일 - Turn 60의 수정본 사용)
    public ChatOrchestrationServiceImpl(model.AiApiService aiApiService, model.PromptService promptService, model.ContextSummarizer contextSummarizer, PlaceDataService placeDataService, NaverApiService naverApiService) {
        this.aiApiService = aiApiService;
        this.promptService = promptService;
        this.contextSummarizer = contextSummarizer;
        this.placeDataService = placeDataService;
        this.naverApiService = naverApiService;
    }

    @Override
    public Mono<ServerMessageResponse> processMessage(ChatContext context, String userMessage) {
        // (0. 맥락 준비 - 동기)
        context.getFullHistory().add("User: " + userMessage);
        context.setTurnCount(context.getTurnCount() + 1);

        // (1. 비동기 요약 시작)
        return contextSummarizer.summarize(
                        context.getSummarizedContext(),
                        context.getFullHistory(),
                        context.getTurnCount()
                )
                .flatMap(summary -> { // 2. 요약이 끝나면
                    context.setSummarizedContext(summary); // 요약본 저장

                    // 3. 상태에 따라 비동기 로직 분기
                    Mono<ServerMessageResponse> responseMono;
                    switch (context.getCurrentState()) {
                        case INITIAL:
                        case COLLECTING_DATA:
                            responseMono = handleDataCollection(context, userMessage, summary);
                            break;
                        case PLACE_QUERY_READY:
                        case RECOMMENDING_PLACE:
                            context.setCurrentState(ChatState.RECOMMENDING_PLACE);
                            responseMono = handlePlaceRecommendation(context, userMessage, summary);
                            break;
                        default:
                            responseMono = Mono.just(new ServerMessageResponse("알 수 없는 오류가 발생했습니다."));
                    }
                    return responseMono;
                })
                .doOnNext(response -> { // 4. 모든 작업이 끝나고 응답이 오면
                    context.getFullHistory().add("AI: " + response.getReplyText());
                })
                .onErrorResume(e -> { // 5. 파이프라인 전체에서 오류 발생 시
                    e.printStackTrace();
                    return Mono.just(new ServerMessageResponse("처리 중 심각한 오류가 발생했습니다: " + e.getMessage()));
                });
    }

    // --- 1단계 로직: 데이터 수집 (비동기) ---
    private Mono<ServerMessageResponse> handleDataCollection(ChatContext context, String userMessage, String summary) {

        // --- (⭐ 핵심 수정 ⭐) ---
        // 현재까지 수집된 데이터를 JSON 문자열로 변환하여 프롬프트에 전달
        String collectedDataJson = "{}"; // 기본값: 빈 JSON 객체
        try {
            if (context.getCollectedData() != null && !context.getCollectedData().isEmpty()) {
                collectedDataJson = objectMapper.writeValueAsString(context.getCollectedData());
            }
        } catch (Exception e) {
            System.err.println("collectedData JSON 변환 실패: " + e.getMessage());
        }

        // 프롬프트 빌드 시 collectedDataJson 추가
        Map<String, Object> data = Map.of(
                "summary", summary,
                "userInput", userMessage,
                "collectedDataJson", collectedDataJson // <--- 이 변수를 프롬프트에 전달
        );
        // --- (수정 끝) ---

        String prompt = promptService.buildPrompt("phase1_collect_data", data);

        return aiApiService.getAiResponse(prompt) // (비동기)
                .map(aiResponseJson -> { // (동기 파싱)
                    try {
                        Phase1Response responseDto = objectMapper.readValue(aiResponseJson, Phase1Response.class);

                        if ("PLACE_QUERY_READY".equalsIgnoreCase(responseDto.getStatus())) {
                            context.setCurrentState(ChatState.PLACE_QUERY_READY);
                            // AI가 최종 정리한 collectedData로 업데이트
                            if (responseDto.getCollectedData() != null) {
                                context.setCollectedData(responseDto.getCollectedData());
                            }
                        }
                        return new ServerMessageResponse(responseDto.getReply());

                    } catch (Exception e) {
                        System.err.println("Phase1 JSON 파싱 실패: " + e.getMessage());
                        System.err.println("AI 응답 원본: " + aiResponseJson); // 원본 로그 출력
                        throw new RuntimeException("AI 응답 처리 중 오류가 발생했습니다.");
                    }
                });
    }

    // --- 2단계 로직: 장소 추천 (비동기) ---
    private Mono<ServerMessageResponse> handlePlaceRecommendation(ChatContext context, String userMessage, String summary) {

        // --- 모듈 1: AI (사용자 의도 파싱) ---
        return aiApiService.parseQuery(summary, userMessage) // (비동기)
                .flatMap(query -> {
                    // --- 모듈 2: 서버 (데이터 수집) ---
                    String searchQuery = query.getLocation() + " " + query.getCategory() + " " + (query.getAtmosphere() != null ? query.getAtmosphere() : "");

                    // 2a. [내부 DB - JPA] (⭐중요: Blocking Call 래핑)
                    Mono<List<Place>> dbPlacesMono = Mono.fromCallable(() -> {
                        System.out.println("[Orchestrator] (JPA-1) DB에서 장소 검색...");
                        List<Place> dbPlaces = placeDataService.getPlacesByLocation(query.getLocation(), query.getRadius());
                        return placeDataService.filterByCategory(dbPlaces, query.getCategory());
                    }).subscribeOn(Schedulers.boundedElastic()); // JPA는 별도 스레드에서 실행

                    // 2b. [외부 Naver Map API] + [AI 파싱] (비동기)
                    Mono<List<Place>> mapPlacesMono = naverApiService.searchLocal(searchQuery) // (비동기)
                            .flatMap(naverMapJson -> aiApiService.parsePlacesFromMapData(naverMapJson)); // (비동기)

                    // 2c. [데이터 결합] (두 비동기 작업이 모두 끝나면)
                    return Mono.zip(dbPlacesMono, mapPlacesMono);
                })
                .flatMap(tuple -> { // tuple.getT1() = dbPlaces, tuple.getT2() = mapPlaces
                    // --- 모듈 2d: [데이터 강화] (비동기 루프) ---
                    List<Place> combinedList = combineAndDeduplicate(tuple.getT1(), tuple.getT2());
                    int limit = Math.min(combinedList.size(), 5);
                    System.out.println("[Orchestrator] 총 " + combinedList.size() + "개 후보 확보, " + limit + "개 강화 시작...");

                    // (List -> Flux로 변환하여 병렬/비동기 처리)
                    return Flux.fromIterable(combinedList.subList(0, limit))
                            .flatMap(place -> // 각 장소를 비동기로 강화
                                    enrichPlace(place, (place.getCategory() != null ? place.getCategory() : "")),
                                    2
                            )
                            .collectList(); // 다시 Mono<List<EnrichedPlace>>로 변환
                })
                .flatMap(enrichedList -> {
                    // --- 모듈 3: AI (최종 분석 및 추천) ---
                    System.out.println("[Orchestrator] 모듈 3: AI 최종 추천 시작...");
                    try {
                        String placeListJson = objectMapper.writeValueAsString(enrichedList);
                        Map<String, Object> recommendData = Map.of(
                                "summary", summary,
                                "userInput", userMessage,
                                "placeListJson", placeListJson
                        );
                        String recommendPrompt = promptService.buildPrompt("phase2_recommend_final", recommendData);

                        return aiApiService.getAiResponse(recommendPrompt); // (비동기)
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                .map(finalResponseJson -> { // (동기 파싱)
                    try {
                        FinalRecommendationResponse finalRec = objectMapper.readValue(finalResponseJson, FinalRecommendationResponse.class);
                        return new ServerMessageResponse(
                                finalRec.getRecommendation(),
                                finalRec.getPros(),
                                finalRec.getCons()
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Final JSON 파싱 실패", e);
                    }
                });
    }

    /** (헬퍼) 비동기 장소 강화 로직 */
    private Mono<EnrichedPlace> enrichPlace(Place place, String category) {
        System.out.println("[Orchestrator] 블로그 리뷰 요약 중: " + place.getName());
        return naverApiService.searchBlogSnippets(place.getName() + " " + category + " 후기") // (비동기)
                .flatMap(snippets -> aiApiService.summarizeReviewSnippets(snippets)) // (비동기)
                .map(aiSummary -> new EnrichedPlace(place, aiSummary)); // (동기 변환)
    }

    /** (헬퍼) 중복 제거 로직 (동기) */
    private List<Place> combineAndDeduplicate(List<Place> list1, List<Place> list2) {
        List<Place> combined = new ArrayList<>(list1);
        combined.addAll(list2);
        return combined.stream()
                .filter(p -> p.getName() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Place::getName, p -> p, (p1, p2) -> p1),
                        Map::values
                ))
                .stream().collect(Collectors.toList());
    }
}