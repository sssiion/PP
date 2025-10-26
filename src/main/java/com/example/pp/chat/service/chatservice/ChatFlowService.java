package com.example.pp.chat.service.chatservice;

import com.example.pp.chat.dto.RecommendedPlace;
import com.example.pp.chat.dto.ServerMessageResponse;
import com.example.pp.chat.dto.UserMessageRequest;
import com.example.pp.chat.repository.*;
import com.example.pp.chat.repository.PlaceWithCongestion;
import com.example.pp.chat.service.external.KakaoLocalClient;
import com.example.pp.chat.util.Geo;
import com.example.pp.rec.dto.CongestionRequestDto;
import com.example.pp.rec.dto.CongestionResponseDto;
import com.example.pp.rec.service.CongestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatFlowService {
    private final KakaoLocalClient kakao;
    private final CongestionService congestionService;
    private final PlaceStore placeStore;

    public Mono<ServerMessageResponse> runPipeline(UserMessageRequest req, WebSession session) {
        String sessionId = Optional.ofNullable(req.getSessionId()).filter(s->!s.isBlank()).orElse(UUID.randomUUID().toString());
        String raw = Optional.ofNullable(req.getMessage()).orElse("").trim();

        return ensureCoordinatesWithKeyword(raw, req.getLat(), req.getLon())
                .switchIfEmpty(Mono.error(new IllegalStateException("NEED_LOCATION")))
                .flatMap(coord -> {
                    double lat = coord.getT1();
                    double lon = coord.getT2();
                    String query = normalizeQuery(raw);

                    // Kakao keyword 검색: x=경도, y=위도, radius, size=15 page=1~2, sort=distance
                    return kakao.searchKeywordPaged(query, lat, lon, 5000) // 문서 규격에 맞는 파라미터 구성
                            .map(docs -> toPlaceBasics(docs, lat, lon))
                            .flatMap(basics -> placeStore.savePlacesBasic(sessionId, basics).thenReturn(basics))
                            .flatMap(basics -> {
                                if (basics.isEmpty()) return Mono.just(List.<PlaceWithCongestion>of());
                                String isoNow = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());
                                List<CongestionRequestDto> reqs = basics.stream()
                                        .map(p -> new CongestionRequestDto(p.getLat(), p.getLon(), isoNow))
                                        .toList();
                                return congestionService.getCongestion(reqs)
                                        .map(cong -> mergeCongestion(basics, cong));
                            })
                            .flatMap(withCongestion -> {
                                List<PlaceWithCongestion> top5 = sortAndPick(withCongestion);
                                session.getAttributes().put(sessionId, top5);
                                return placeStore.savePlacesWithCongestion(sessionId, top5)
                                        .thenReturn(ServerMessageResponse.ok("추천 결과를 정리했어요.", List.of(), List.of(), toRecommendedPlaces(top5)));
                            });
                })
                .onErrorResume(ex -> {
                    if ("NEED_LOCATION".equals(ex.getMessage())) {
                        return Mono.just(ServerMessageResponse.text("좌표가 없어요. [translate:홍대입구역], [translate:강남역] 같은 기준 위치를 말하거나 현재 위치를 공유해 주세요."));
                    }
                    return Mono.just(ServerMessageResponse.error("일시적 오류가 발생했어요. 잠시 후 다시 시도해주세요."));
                });
    }

    // 변경: address 지오코딩 제거. keyword만으로 좌표를 확보.
    private Mono<Tuple2<Double,Double>> ensureCoordinatesWithKeyword(String message, Double lat, Double lon) {
        if (lat!=null && lon!=null) return Mono.just(Tuples.of(lat, lon));
        String place = extractPlace(message);
        if (place==null || place.isBlank()) return Mono.empty();

        // 서울시청을 임시 중심으로 keyword 검색 1페이지 거리정렬 → 첫 문서의 y/x로 좌표 확보
        double centerLat = 37.5665, centerLon = 126.9780;
        return kakao.searchKeywordPaged(place, centerLat, centerLon, 20000)
                .map(docs -> docs.stream().findFirst()
                        .map(doc -> Tuples.of(parseDouble(doc.get("y")), parseDouble(doc.get("x"))))
                        .orElse(null))
                .flatMap(coord -> coord != null ? Mono.just(coord) : Mono.empty());
    }

    private String normalizeQuery(String raw) {
        return raw.isBlank() ? "카페" : raw;
    }

    private List<PlaceBasic> toPlaceBasics(List<Map<String,Object>> docs, double userLat, double userLon) {
        return docs.stream().map(d -> {
                    String id = String.valueOf(d.get("id"));
                    String name = String.valueOf(d.getOrDefault("place_name",""));
                    String addr = String.valueOf(d.getOrDefault("road_address_name", d.getOrDefault("address_name","")));
                    double lon = parseDouble(d.get("x")); // Kakao: x=경도
                    double lat = parseDouble(d.get("y")); // Kakao: y=위도
                    String dist = String.valueOf(d.getOrDefault("distance",""));
                    double km = computeDistanceKm(userLat, userLon, lat, lon, dist);
                    return new PlaceBasic(id, name, lat, lon, addr, km);
                }).sorted(Comparator.comparingDouble(PlaceBasic::getDistanceKm))
                .limit(30)
                .toList();
    }

    private double computeDistanceKm(double userLat, double userLon, double lat, double lon, String kakaoDistanceMeters) {
        if (kakaoDistanceMeters!=null && kakaoDistanceMeters.matches("\\d+")) {
            return Math.round((Integer.parseInt(kakaoDistanceMeters) / 1000.0) * 100.0)/100.0;
        }
        double meters = Geo.haversineMeters(userLat, userLon, lat, lon);
        return Math.round((meters / 1000.0) * 100.0)/100.0;
    }

    private List<PlaceWithCongestion> mergeCongestion(List<PlaceBasic> basics, List<CongestionResponseDto> cong) {
        Map<String, String> byKey = cong.stream().collect(Collectors.toMap(
                c -> fixedKey(c.getLatitude(), c.getLongitude()),
                CongestionResponseDto::getCongestionLevel,
                (a,b)->a
        ));
        return basics.stream().map(p -> {
            String level = byKey.getOrDefault(fixedKey(p.getLat(), p.getLon()), "정보없음");
            return new PlaceWithCongestion(p.getId(), p.getName(), p.getLat(), p.getLon(), p.getAddress(), p.getDistanceKm(), level);
        }).toList();
    }

    private String fixedKey(double lat, double lon) {
        return String.format(Locale.ROOT, "%.6f,%.6f", lat, lon);
    }

    private List<PlaceWithCongestion> sortAndPick(List<PlaceWithCongestion> list) {
        Map<String,Integer> order = Map.of("여유",1,"보통",2,"약간 붐빔",3,"붐빔",4,"정보없음",5);
        return list.stream()
                .sorted(Comparator
                        .comparing((PlaceWithCongestion p) -> order.getOrDefault(p.getCongestionLevel(), 5))
                        .thenComparingDouble(PlaceWithCongestion::getDistanceKm))
                .limit(5)
                .toList();
    }

    private String extractPlace(String m) {
        if (m==null) return null;
        Matcher pm = Pattern.compile("([가-힣A-Za-z0-9]+(?:역|동|구|시|군|면)?)").matcher(m);
        if (pm.find()) return pm.group(1);
        return null;
    }

    private double parseDouble(Object v) {
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; }
    }

    private List<RecommendedPlace> toRecommendedPlaces(List<PlaceWithCongestion> src){
        return src.stream().map(s -> {
            RecommendedPlace r = new RecommendedPlace();
            r.setId(s.getId());
            r.setName(s.getName());
            r.setAddress(s.getAddress());
            r.setLatitude(s.getLat());
            r.setLongitude(s.getLon());
            r.setCongestionLevel(s.getCongestionLevel());
            r.setDistance(s.getDistanceKm());
            return r;
        }).toList();
    }
}
