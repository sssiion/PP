package com.example.pp.chat.service.chatservice;


import com.example.pp.chat.dto.PlaceDetails;
import com.example.pp.chat.dto.RecommendedPlace;
import com.example.pp.chat.dto.ServerMessageResponse;
import com.example.pp.chat.dto.UserMessageRequest;
import com.example.pp.chat.entity.ChatContext;
import com.example.pp.chat.entity.ChatState;
import com.example.pp.chat.mapper.PlaceMapper;
import com.example.pp.chat.repository.InMemoryChatContextRepository;
import com.example.pp.chat.service.ai.AiApiService;
import com.example.pp.chat.service.external.KakaoApiService;
import com.example.pp.chat.service.external.NaverApiService;
import com.example.pp.chat.service.rank.RankingService;
import com.example.pp.chat.util.Geo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final AiApiService ai;
    private final KakaoApiService kakao;
    private final NaverApiService naver;
    private final RankingService ranking;
    private final InMemoryChatContextRepository contextRepo;
    private final ObjectMapper om = new ObjectMapper();
    private final Optional<io.micrometer.core.instrument.Timer> chatTimerOpt;

    public Mono<ServerMessageResponse> processChatRequest(UserMessageRequest req, WebSession session){
        long start = System.nanoTime();

        // 0) 맨 앞 분기: 모든 입력에 즉시 반응(간단 리액션) + 예시 패턴 빠른 감지
        ServerMessageResponse quick = quickReaction(req);
        // 장소 의도 간단 패턴 감지
        boolean quickPlace = isPlacePattern(req.getMessage());

        // 즉답 모드: 일단 반응 반환을 보장
        Mono<ServerMessageResponse> baseFlow = Mono.just(quick);

        // 실제 파이프라인
        Mono<ServerMessageResponse> placeFlow = loadOrInitContext(req)
                .flatMap(ctx -> step1Intent(ctx, req))                       // LLM 의도
                .flatMap(tuple -> step2Location(tuple.getT1(), req, tuple.getT2()))
                .flatMap(ctx -> step3Congestion(ctx))
                .flatMap(ctx -> step4Preferences(ctx, req))
                .flatMap(ctx -> step5Search(ctx, req))
                .flatMap(tuple -> step6Enrich(tuple.getT1(), tuple.getT2()))
                .flatMap(enriched -> step7Select(enriched))
                .flatMap(top5 -> step8SaveAndRespond(req, top5, session))
                .onErrorResume(ex -> fallbackNeedLocationOrGeneric(ex, req, session));

        // 전략: 패턴으로 장소 의도 맞으면 바로 파이프라인, 아니면 의도 결과 보고 장소일 때만 파이프라인
        Mono<ServerMessageResponse> result = quickPlace
                ? placeFlow.defaultIfEmpty(quick)
                : baseFlow.flatMap(q -> placeFlow
                .onErrorResume(e -> Mono.empty())
                .defaultIfEmpty(q));

        return result
                .doOnTerminate(() -> chatTimerOpt.ifPresent(t ->
                        t.record(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS)));
    }
    // 빠른 반응(모든 입력에 항상 1줄 이상)
    private ServerMessageResponse quickReaction(UserMessageRequest req){
        String msg = Optional.ofNullable(req.getMessage()).orElse("").trim();
        String reply;
        if (msg.isBlank()) {
            reply = "무엇을 도와드릴까요? 예: ‘강남역 카페 알려줘’, ‘홍대 혼잡도 낮은 맛집’";
        } else if (isGreeting(msg)) {
            reply = "안녕하세요! 위치나 키워드를 알려주시면 근처 추천을 찾아드릴게요.";
        } else if (isHelp(msg)) {
            reply = "예시) ‘강남역 카페 알려줘’, ‘홍대 저녁 한식’, ‘현재 위치 근처 조용한 서점’처럼 요청해 보세요.";
        } else if (isPlacePattern(msg)) {
            reply = "요청을 확인했어요! 가까운 곳을 탐색해 추천을 준비할게요.";
        } else {
            reply = "좋아요! 근처에서 원하는 장소가 있나요? 예: ‘강남역 카페’ ‘홍대 혼잡 낮은 맛집’";
        }
        return ServerMessageResponse.text(reply);
    }
    private boolean isGreeting(String m){
        String s = m.toLowerCase();
        return s.contains("안녕") || s.matches("^(hi|hello|hey)$");
    }
    private boolean isHelp(String m){
        String s = m.toLowerCase();
        return s.contains("도움") || s.contains("help") || s.contains("사용법");
    }
    // “강남역 카페 알려줘”, “홍대 저녁 한식”, “잠실 카페 추천” 등 장소+키워드 패턴 간단 감지
    private boolean isPlacePattern(String m){
        if (m == null) return false;
        String s = m.replaceAll("\\s+", "");
        return s.matches(".*(역|동|구|시|군|면|로|길).*(맛집|카페|식당|서점|공원|바|베이커리|디저트|편의점|관광|명소|스팟|추천|알려줘).*")
                || s.matches(".*(맛집|카페|식당|서점|공원|바|베이커리|디저트|편의점|관광|명소|스팟).*(추천|알려줘).*");
    }

    // 0) 컨텍스트 로드
    private Mono<ChatContext> loadOrInitContext(UserMessageRequest req){
        String sid = Optional.ofNullable(req.getSessionId()).orElse(UUID.randomUUID().toString());
        return contextRepo.findById(sid)
                .switchIfEmpty(Mono.fromCallable(() -> {
                    ChatContext c = new ChatContext();
                    c.setSessionId(sid);
                    c.setState(ChatState.AWAITING_LOCATION_CHOICE.name());
                    return c;
                }))
                .flatMap(contextRepo::save);
    }

    // 1) 의도 파악(LLM + null-safe vars)
    private Mono<Tuple2<ChatContext, JsonNode>> step1Intent(ChatContext ctx, UserMessageRequest req){
        Double lat = req.getLat();
        Double lon = req.getLon();
        String timeStr = Optional.ofNullable(req.getTime()).orElse(LocalTime.now()).toString();

        Map<String,Object> vars = new HashMap<>();
        vars.put("message", Optional.ofNullable(req.getMessage()).orElse(""));
        if (lat != null) vars.put("lat", lat);
        if (lon != null) vars.put("lon", lon);
        vars.put("time", timeStr);

        return ai.completeAndValidate("check_intent_v2.txt", vars, "intent.schema.json")
                .map(json -> Tuples.of(ctx, json))
                .onErrorResume(e -> Mono.just(Tuples.of(ctx, defaultIntentJson())));
    }

    private JsonNode defaultIntentJson(){
        return om.createObjectNode()
                .put("intent","UNKNOWN")
                .put("mentionedNewPlace", false)
                .put("placeName", (String) null)
                .put("needLocation", false);
    }

    // 2) 위치 확정(NEED_LOCATION 명시 오류로 유도)
    private Mono<ChatContext> step2Location(ChatContext ctx, UserMessageRequest req, JsonNode intent){
        boolean mentioned = intent.path("mentionedNewPlace").asBoolean(false);
        String placeName = intent.path("placeName").isTextual()? intent.path("placeName").asText(): null;

        if ((ctx.getCurrentSearchLat()==null || ctx.getCurrentSearchLon()==null)
                && (req.getLat()==null || req.getLon()==null)
                && (placeName==null || placeName.isBlank())) {
            return Mono.error(new IllegalStateException("NEED_LOCATION"));
        }

        if (mentioned && placeName != null && !placeName.isBlank()){
            return kakao.geocodeByAddress(placeName)
                    .map(list -> {
                        if (!list.isEmpty()){
                            Map<String,Object> first = list.get(0);
                            double lat = parseDouble(first, "y");
                            double lon = parseDouble(first, "x");
                            ctx.setCurrentSearchLat(lat);
                            ctx.setCurrentSearchLon(lon);
                        } else if (req.getLat()!=null && req.getLon()!=null){
                            ctx.setCurrentSearchLat(req.getLat());
                            ctx.setCurrentSearchLon(req.getLon());
                        }
                        ctx.setState(ChatState.AWAITING_CONGESTION.name());
                        return ctx;
                    })
                    .onErrorReturn(ctx);
        }
        if (req.getLat()!=null && req.getLon()!=null){
            ctx.setCurrentSearchLat(req.getLat());
            ctx.setCurrentSearchLon(req.getLon());
            ctx.setState(ChatState.AWAITING_CONGESTION.name());
            return Mono.just(ctx);
        }
        ctx.setState(ChatState.AWAITING_LOCATION_CHOICE.name());
        return Mono.just(ctx);
    }

    private double parseDouble(Map<String,Object> m, String key){
        try { return Double.parseDouble(String.valueOf(m.getOrDefault(key, "0"))); }
        catch (Exception e){ return 0.0; }
    }

    // 3) 혼잡도 기본값
    private Mono<ChatContext> step3Congestion(ChatContext ctx){
        if (ctx.getCongestionPreference()==null){
            ctx.setCongestionPreference("보통");
        }
        ctx.setState(ChatState.COLLECTING_PREFERENCE.name());
        return contextRepo.save(ctx);
    }

    // 4) 선호 키워드 수집/병합(null-safe)
    private Mono<ChatContext> step4Preferences(ChatContext ctx, UserMessageRequest req){
        if (ctx.getPreferenceKeywords()==null || ctx.getPreferenceKeywords().isEmpty()){
            Map<String,Object> vars = new HashMap<>();
            vars.put("userMessage", Optional.ofNullable(req.getMessage()).orElse(""));
            return ai.completeAndValidate("ask_preference.txt", vars, "preferences.schema.json")
                    .map(json -> {
                        Map<String,String> pref = new HashMap<>();
                        json.fieldNames().forEachRemaining(f -> {
                            String v = json.path(f).isNull()? null : json.path(f).asText(null);
                            pref.put(f, v);
                        });
                        ctx.setPreferenceKeywords(pref);
                        ctx.setState(ChatState.READY.name());
                        return ctx;
                    })
                    .onErrorReturn(ctx);
        }
        ctx.setState(ChatState.READY.name());
        return contextRepo.save(ctx);
    }

    // 5) 검색
    private Mono<Tuple2<ChatContext,List<RecommendedPlace>>> step5Search(ChatContext ctx, UserMessageRequest req){
        double lat = Optional.ofNullable(ctx.getCurrentSearchLat()).orElse(Optional.ofNullable(req.getLat()).orElse(0.0));
        double lon = Optional.ofNullable(ctx.getCurrentSearchLon()).orElse(Optional.ofNullable(req.getLon()).orElse(0.0));
        String q = Optional.ofNullable(req.getMessage()).orElse("").trim();

        if ((lat==0.0 && lon==0.0) && q.isBlank()){
            return Mono.just(Tuples.of(ctx, List.of())); // 정보 부족
        }

        Mono<List<Map<String,Object>>> kakaoMono =
                kakao.searchByKeyword(q, lat, lon, 30, 5000).onErrorReturn(List.of());
        Mono<List<Map<String,Object>>> naverMono =
                naver.searchLocal(q, 10, 1, "sim").onErrorReturn(List.of());

        return Mono.zip(kakaoMono, naverMono)
                .map(zip -> {
                    var kakaoPlaces = PlaceMapper.fromKakaoDocuments(zip.getT1());
                    var naverPlaces = PlaceMapper.fromNaverLocal(zip.getT2());
                    List<RecommendedPlace> merged = new ArrayList<>(kakaoPlaces);
                    merged.addAll(naverPlaces);

                    for (RecommendedPlace p : merged){
                        if (p.getLatitude()!=0.0 || p.getLongitude()!=0.0){
                            double meters = Geo.haversineMeters(lat, lon, p.getLatitude(), p.getLongitude());
                            p.setDistance(Geo.metersToKmRounded2(meters));
                        }
                        if (p.getCongestionLevel()==null) p.setCongestionLevel("정보없음");
                    }

                    List<RecommendedPlace> top3 = merged.stream()
                            .filter(p -> p.getDistance()>0)
                            .sorted(Comparator.comparingDouble(RecommendedPlace::getDistance))
                            .limit(3)
                            .collect(Collectors.toList());

                    return Tuples.of(ctx, top3);
                })
                .onErrorReturn(Tuples.of(ctx, List.of()));
    }

    // 6) 데이터 강화(카카오 재검증 + 네이버 블로그 스니펫 + AI 요약)
    private Mono<List<RecommendedPlace>> step6Enrich(ChatContext ctx, List<RecommendedPlace> list){
        if (list==null || list.isEmpty()) return Mono.just(List.of());

        return Flux.fromIterable(list).flatMap(p -> {
            String query = p.getName()+" "+p.getAddress();
            Mono<List<Map<String,Object>>> kakaoVerify = kakao.searchByKeyword(query,
                    Optional.ofNullable(ctx.getCurrentSearchLat()).orElse(0.0),
                    Optional.ofNullable(ctx.getCurrentSearchLon()).orElse(0.0),
                    5, 3000);

            Mono<List<String>> blog = naver.searchBlogSnippets(query, 6, 1, "sim");

            return Mono.zip(kakaoVerify, blog).flatMap(zip -> {
                // 카카오 재검증: 가장 유사한 하나로 필드 보강(단순 우선 1개)
                if (!zip.getT1().isEmpty()){
                    Map<String,Object> doc = zip.getT1().get(0);
                    p.setName((String) doc.getOrDefault("place_name", p.getName()));
                    p.setAddress((String) doc.getOrDefault("road_address_name", p.getAddress()));
                    try {
                        double lat = Double.parseDouble(String.valueOf(doc.getOrDefault("y",""+p.getLatitude())));
                        double lon = Double.parseDouble(String.valueOf(doc.getOrDefault("x",""+p.getLongitude())));
                        p.setLatitude(lat); p.setLongitude(lon);
                    } catch (Exception ignore){}
                }
                Map<String,Object> vars = Map.of("kakao", p, "blogSnippets", zip.getT2());
                return ai.complete("enrich_place_data.txt", vars).map(raw -> {
                    p.setData(parseDetails(raw));
                    return p;
                });
            }).onErrorReturn(p);
        }, 2).collectList();
    }

    private PlaceDetails parseDetails(String raw){
        try {
            JsonNode n = om.readTree(raw);
            List<String> go = new ArrayList<>();
            n.path("reasonsToGo").forEach(x -> go.add(x.asText()));
            List<String> not = new ArrayList<>();
            n.path("reasonsNotToGo").forEach(x -> not.add(x.asText()));
            return new PlaceDetails(n.path("placeSummary").asText(), go, not);
        } catch (Exception e){
            return new PlaceDetails("요약 준비 중", List.of(), List.of());
        }
    }

    // 7) 최종 선별(규칙 기반 폴백)
    private Mono<List<RecommendedPlace>> step7Select(List<RecommendedPlace> enriched){
        List<RecommendedPlace> dedup = ranking.dedup(enriched);
        List<RecommendedPlace> sorted = ranking.sortByCongestionThenDistance(dedup);
        return Mono.just(sorted.stream().limit(5).toList());
    }

    // 8) 저장/응답 + ‘검색 결과 없음’ 대체 답변
    private Mono<ServerMessageResponse> step8SaveAndRespond(UserMessageRequest req, List<RecommendedPlace> top5, WebSession session){
        String sid = Optional.ofNullable(req.getSessionId())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
        List<RecommendedPlace> safeList = (top5 == null) ? List.of() : top5;
        session.getAttributes().put(sid, safeList);

        if (safeList.isEmpty()){
            String tip = "근처에서 적합한 결과가 없어요. 반경을 넓히거나 다른 키워드를 시도해볼까요? 예: ‘반경 10km’, ‘조용한 카페’, ‘공원’";
            return Mono.just(ServerMessageResponse.text(tip));
        }
        return Mono.just(ServerMessageResponse.from(safeList));
    }

    private Mono<ServerMessageResponse> fallbackNeedLocationOrGeneric(Throwable ex, UserMessageRequest req, WebSession session){
        if ("NEED_LOCATION".equals(ex.getMessage())){
            return Mono.just(ServerMessageResponse.text("기준 위치가 필요해요. 좌표를 보내거나 ‘강남역’ 같은 위치를 알려주세요."));
        }
        return fallbackBasicResponse(req, session);
    }

    private Mono<ServerMessageResponse> fallbackBasicResponse(UserMessageRequest req, WebSession session){
        String sid = Optional.ofNullable(req.getSessionId()).orElse("sid");
        session.getAttributes().put(sid, List.of());
        return Mono.just(ServerMessageResponse.error("일시적 오류가 발생했어요. 잠시 후 다시 시도해주세요."));
    }

}
