package com.example.pp.chat.service.chatservice;


import com.example.pp.chat.dto.*;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Set<String> CATEGORY_WORDS = Set.of(
            "카페","맛집","식당","한식","양식","중식","일식",
            "베이커리","디저트","술집","서점","공원","관광","명소"
    );

    private static final Pattern PLACE_HINT =
            Pattern.compile("([가-힣A-Za-z0-9]+(?:역|동|구|시|군|면))");


    public Mono<ServerMessageResponse> processChatRequest(UserMessageRequest req, WebSession session) {
        return loadOrInitContext(req)
                .flatMap(ctx -> ai.parseIntentAndEntities(req.getMessage(), session)
                        .flatMap(intent -> {
                            // 상태변경 여부 판단 (자료 확보 후 결정)
                            boolean changedTemp = false;
                            Map<String, String> prefKeywords = Optional.ofNullable(ctx.getPreferenceKeywords()).orElse(new HashMap<>());

                            // 반경 값 비교
                            if (intent.getRadius() != null && !intent.getRadius().toString().equals(prefKeywords.get("radius"))) {
                                prefKeywords.put("radius", intent.getRadius().toString());
                                changedTemp = true;
                            }

                            // 장소명 비교
                            if (intent.getPlaceName() != null && !intent.getPlaceName().equals(ctx.getLastPlaceName())) {
                                ctx.setLastPlaceName(intent.getPlaceName());
                                changedTemp = true;
                            }

                            // 키워드 비교
                            String newKeywords = String.join(",", intent.getKeywords() == null ? List.of() : intent.getKeywords());
                            if (!newKeywords.equals(ctx.getLastCategory())) {
                                ctx.setLastCategory(newKeywords);
                                changedTemp = true;
                            }

                            // 세션 저장
                            final boolean finalChanged = changedTemp; // final 선언 후 람다 내부에서 읽기전용
                            return contextRepo.save(ctx)
                                    .flatMap(savedCtx -> {
                                        if (!intent.isReady()) {
                                            return Mono.just(ServerMessageResponse.text(intent.getAskWhatIsMissing()));
                                        }
                                        if ("READY".equals(savedCtx.getState()) && !finalChanged) {
                                            return Mono.just(ServerMessageResponse.text("조건이 바뀌지 않아 검색이 반복되지 않습니다. 다른 조건을 말씀해 주세요."));
                                        }
                                        savedCtx.setState("READY");
                                        return contextRepo.save(savedCtx)
                                                .then(callExternalApis(intent))
                                                .flatMap(extResult -> ai.summarizePlacesAndBlogs(intent, extResult)
                                                        .map(ServerMessageResponse::text));
                                    });
                        })
                );
    }






    public Mono<ParsedIntent> parseIntentAndEntities(String userMessage, Object context) {
        ParsedIntent intent = new ParsedIntent();
        // 예: 반경 추출 (단위 km, m 모두 대응)
        Pattern radiusPat = Pattern.compile("반경 ?([0-9]+)\\s*(km|m)");
        Matcher m = radiusPat.matcher(userMessage);
        if (m.find()) {
            int r = Integer.parseInt(m.group(1));
            intent.setRadius(m.group(2).equals("km") ? r * 1000 : r);
        }
        // 장소명 추출 - 간단 정규식
        Pattern placePat = Pattern.compile("([가-힣A-Za-z0-9]+(역|동|구|시|군|면))");
        Matcher pm = placePat.matcher(userMessage);
        if (pm.find()) {
            intent.setPlaceName(pm.group(1));
        }
        // 키워드 예시 추출
        List<String> keywords = new ArrayList<>();
        for (String k : List.of("카페","맛집","조용한","아기자기","서점","공원")) {
            if (userMessage.contains(k)) keywords.add(k);
        }
        intent.setKeywords(keywords);

        // ready 판정: 장소명 또는 키워드가 있으면 true
        intent.setReady((intent.getPlaceName() != null && !intent.getPlaceName().isBlank()) || !keywords.isEmpty());

        if (!intent.isReady()) intent.setAskWhatIsMissing("원하시는 장소나 조건을 좀 더 알려주세요. 예) ‘홍대역 조용한 카페’");
        return Mono.just(intent);
    }
    private Mono<ExternalSearchResult> callExternalApis(ParsedIntent intent) {
        double lat = intent.getLat() != null ? intent.getLat() : 0.0;
        double lon = intent.getLon() != null ? intent.getLon() : 0.0;
        int radius = intent.getRadius() != null ? intent.getRadius() : 3000;
        String key = String.join(" ",
                intent.getPlaceName() == null ? "" : intent.getPlaceName(),
                String.join(" ", intent.getKeywords() == null ? List.of() : intent.getKeywords())
        );
        Mono<List<RecommendedPlace>> kakaoMono = kakao.searchByKeyword(key, lat, lon, 10, radius)
                .map(PlaceMapper::fromKakaoDocuments).onErrorReturn(List.of());
        Mono<List<BlogReview>> blogMono = naver.searchBlogReviewsAsParsed(key, 5, 1, "sim").onErrorReturn(List.of());

        return Mono.zip(kakaoMono, blogMono)
                .map(zip -> {
                    ExternalSearchResult ext = new ExternalSearchResult();
                    ext.setPlaces(zip.getT1());
                    ext.setBlogs(zip.getT2());
                    return ext;
                });
    }


    private ServerMessageResponse quickReaction(UserMessageRequest req, ChatContext ctx){
        String msg = Optional.ofNullable(req.getMessage()).orElse("").trim();
        String reply = msg.isBlank()
                ? "무엇을 도와드릴까요? 예: ‘강남역 카페 알려줘’"
                : (isPlacePattern(msg) ? "요청 확인! 근처 추천을 준비할게요." : "좋아요! 원하시는 위치/키워드를 알려주시면 찾아드릴게요.");
        return ServerMessageResponse.text(reply);
    }

    // 장소/카테고리 패턴 정규식
    private boolean isPlacePattern(String m){
        if (m==null) return false;
        String s=m.replaceAll("\\s+","");
        return s.matches(".*(역|동|구|시|군|면).*(맛집|카페|식당|서점|공원|명소|추천|알려줘).*")
                || s.matches(".*(맛집|카페|식당|서점|공원|명소).*(추천|알려줘).*");
    }

    // 현재 발화에서 엔티티 추출하여 컨텍스트 갱신
    private void extractAndMemoEntities(ChatContext ctx, String message){
        if (message==null) return;
        CATEGORY_WORDS.stream().filter(message::contains).findFirst()
                .ifPresent(ctx::setLastCategory);
        Matcher m = PLACE_HINT.matcher(message);
        if (m.find()) ctx.setLastPlaceName(m.group(1));
    }

    private Mono<ServerMessageResponse> placeFlow(ChatContext ctx, UserMessageRequest req, WebSession session){
        return step1Intent(ctx, req)
                .flatMap(tuple -> step2Location(tuple.getT1(), req, tuple.getT2()))
                .flatMap(c -> step5Search(c, req))
                .flatMap(tuple -> step6Enrich(tuple.getT1(), tuple.getT2()))
                .flatMap(enriched -> step7Select(enriched))
                .flatMap(top5 -> step8SaveAndRespond(req, top5, session));
    }

    // 세션 컨텍스트 불러오기/초기화
    public Mono<ChatContext> loadOrInitContext(UserMessageRequest req) {
        String sid = Optional.ofNullable(req.getSessionId())
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        return contextRepo.findById(sid)
                .switchIfEmpty(Mono.defer(() -> {
                    ChatContext ctx = new ChatContext();
                    ctx.setSessionId(sid);  // 반드시 sessionId 세팅
                    ctx.setState(ChatState.AWAITING_LOCATION_CHOICE.name());
                    ctx.setPreferenceKeywords(new HashMap<>());
                    return contextRepo.save(ctx);
                }));
    }

    private Mono<Tuple2<ChatContext, JsonNode>> step1Intent(ChatContext ctx, UserMessageRequest req){
        Double lat = ctx.getCurrentSearchLat();
        Double lon = ctx.getCurrentSearchLon();
        String timeStr = Optional.ofNullable(req.getTime()).orElse(LocalTime.now()).toString();
        Map<String,Object> vars = new HashMap<>();
        vars.put("message", Optional.ofNullable(req.getMessage()).orElse(""));
        if (lat!=null) vars.put("lat", lat);
        if (lon!=null) vars.put("lon", lon);
        vars.put("time", timeStr);
        vars.put("stateSummary", stateSummary(ctx));
        return ai.completeAndValidate("check_intent_v2.txt", vars, "intent.schema.json")
                .map(json -> Tuples.of(ctx, json))
                .onErrorResume(e -> Mono.just(Tuples.of(ctx, defaultIntentJson())));
    }

    private String stateSummary(ChatContext c){
        return String.format(
                "context: base_place=%s, lat=%s, lon=%s, category=%s, prefs=%s",
                nn(c.getLastPlaceName()), nn(c.getCurrentSearchLat()), nn(c.getCurrentSearchLon()),
                nn(c.getLastCategory()), nnMap(c.getPreferenceKeywords()));
    }
    private String nn(Object o){ return o==null? "": String.valueOf(o); }
    private String nnMap(Map<String,String> m){ return (m==null||m.isEmpty())? "{}": m.toString(); }

    private JsonNode defaultIntentJson(){
        return om.createObjectNode().put("intent","UNKNOWN")
                .put("mentionedNewPlace", false).put("placeName",(String)null).put("needLocation", false);
    }

    // 좌표/지명 자동 보강, 기준 안내 반복 방지
    private Mono<ChatContext> step2Location(ChatContext ctx, UserMessageRequest req, JsonNode intent){
        boolean hasCoord = ctx.getCurrentSearchLat()!=null && ctx.getCurrentSearchLon()!=null;
        String place = Optional.ofNullable(intent.path("placeName").asText(null)).orElse(ctx.getLastPlaceName());

        if (!hasCoord && place!=null && !place.isBlank()){
            return kakao.geocodeByAddress(place).map(list -> {
                if (!list.isEmpty()){
                    Map<String,Object> first = list.get(0);
                    ctx.setCurrentSearchLat(parseDouble(first,"y"));
                    ctx.setCurrentSearchLon(parseDouble(first,"x"));
                    ctx.setLastPlaceName(place);
                }
                ctx.setState(ChatState.AWAITING_CONGESTION.name());
                return ctx;
            }).onErrorReturn(ctx);
        }
        if (req.getLat()!=null && req.getLon()!=null){
            ctx.setCurrentSearchLat(req.getLat()); ctx.setCurrentSearchLon(req.getLon());
            ctx.setState(ChatState.AWAITING_CONGESTION.name());
            return Mono.just(ctx);
        }
        if (ctx.getCurrentSearchLat()==null || ctx.getCurrentSearchLon()==null){
            if (!Boolean.TRUE.equals(ctx.getFlags().get("askedLocation"))){
                ctx.getFlags().put("askedLocation", true);
                return Mono.error(new IllegalStateException("NEED_LOCATION"));
            }
        }
        ctx.setState(ChatState.AWAITING_CONGESTION.name());
        return Mono.just(ctx);
    }

    private double parseDouble(Map<String,Object> m, String key){
        try { return Double.parseDouble(String.valueOf(m.getOrDefault(key,"0"))); }
        catch (Exception e){ return 0.0; }
    }

    // 장소 카테고리 자동 보강, 네이버/카카오 통합
    private Mono<Tuple2<ChatContext,List<RecommendedPlace>>> step5Search(ChatContext ctx, UserMessageRequest req){
        double lat = Optional.ofNullable(ctx.getCurrentSearchLat()).orElse(Optional.ofNullable(req.getLat()).orElse(0.0));
        double lon = Optional.ofNullable(ctx.getCurrentSearchLon()).orElse(Optional.ofNullable(req.getLon()).orElse(0.0));
        String base = Optional.ofNullable(req.getMessage()).orElse("").trim();
        String cat = Optional.ofNullable(ctx.getLastCategory()).orElse("");
        String q = base; if (!cat.isBlank() && !base.contains(cat)) q = (base+" "+cat).trim();

        if ((lat==0.0 && lon==0.0) && q.isBlank()) return Mono.just(Tuples.of(ctx, List.of()));

        Mono<List<Map<String,Object>>> kakaoMono = kakao.searchByKeyword(q, lat, lon, 30, 5000).onErrorReturn(List.of());
        Mono<List<Map<String,Object>>> naverMono = naver.searchLocal(q, 10, 1, "sim").onErrorReturn(List.of());

        return Mono.zip(kakaoMono, naverMono).map(zip -> {
            var kakaoPlaces = PlaceMapper.fromKakaoDocuments(zip.getT1());
            var naverPlaces = PlaceMapper.fromNaverLocal(zip.getT2());
            List<RecommendedPlace> merged = new ArrayList<>(kakaoPlaces); merged.addAll(naverPlaces);

            for (RecommendedPlace p: merged){
                if (p.getLatitude()!=0.0 || p.getLongitude()!=0.0){
                    double meters = Geo.haversineMeters(lat, lon, p.getLatitude(), p.getLongitude());
                    p.setDistance(Geo.metersToKmRounded2(meters));
                }
                if (p.getCongestionLevel()==null) p.setCongestionLevel("정보없음");
            }
            List<RecommendedPlace> top3 = merged.stream()
                    .filter(p -> p.getDistance()>0)
                    .sorted(Comparator.comparingDouble(RecommendedPlace::getDistance))
                    .limit(3).collect(Collectors.toList());
            return Tuples.of(ctx, top3);
        }).onErrorReturn(Tuples.of(ctx, List.of()));
    }

    // 네이버 블로그 요약 연계 강화(필요시)
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
                Map<String,Object> vars = new HashMap<>();
                vars.put("kakao", p); vars.put("blogSnippets", zip.getT2());
                return ai.complete("enrich_place_data.txt", vars).map(raw -> { p.setData(parseDetails(raw)); return p; });
            }).onErrorReturn(p);
        }, 2).collectList();
    }

    private PlaceDetails parseDetails(String raw){
        try {
            JsonNode n = om.readTree(raw);
            List<String> go=new ArrayList<>(); n.path("reasonsToGo").forEach(x->go.add(x.asText()));
            List<String> not=new ArrayList<>(); n.path("reasonsNotToGo").forEach(x->not.add(x.asText()));
            return new PlaceDetails(n.path("placeSummary").asText("요약 준비 중"), go, not);
        } catch (Exception e){ return new PlaceDetails("요약 준비 중", List.of(), List.of()); }
    }

    // 혼잡도+거리 기준으로 5개 최종 추려서 추천
    private Mono<List<RecommendedPlace>> step7Select(List<RecommendedPlace> enriched){
        List<RecommendedPlace> dedup = ranking.dedup(enriched);
        List<RecommendedPlace> sorted = ranking.sortByCongestionThenDistance(dedup);
        return Mono.just(sorted.stream().limit(5).toList());
    }

    // 응답 구성, 결과 없음 안내 자동(문맥히)
    private Mono<ServerMessageResponse> step8SaveAndRespond(UserMessageRequest req, List<RecommendedPlace> top5, WebSession session){
        String sid = Optional.ofNullable(req.getSessionId()).filter(s->!s.isBlank()).orElse(UUID.randomUUID().toString());
        List<RecommendedPlace> safe = (top5==null)? List.of(): top5;
        session.getAttributes().put(sid, safe);
        if (safe.isEmpty()){
            String where = Optional.ofNullable(req.getMessage()).orElse("해당 지역");
            String tip = where+" 근처에서 적합한 결과가 없어요. 반경을 넓히거나 조건을 바꿔볼까요? 예: ‘반경 10km’, ‘조용한 카페’.";
            return Mono.just(ServerMessageResponse.text(tip));
        }
        return Mono.just(ServerMessageResponse.from(safe));
    }

    private Mono<ServerMessageResponse> fallbackNeedLocationOrGeneric(Throwable ex, UserMessageRequest req, WebSession session){
        if ("NEED_LOCATION".equals(ex.getMessage())){
            return Mono.just(ServerMessageResponse.text("좌표가 없어요. ‘강남역’, ‘시청역’ 같은 기준 위치를 말하거나 현재 위치를 공유해 주세요."));
        }
        return Mono.just(ServerMessageResponse.error("일시적 오류가 발생했어요. 잠시 후 다시 시도해주세요."));
    }
}

