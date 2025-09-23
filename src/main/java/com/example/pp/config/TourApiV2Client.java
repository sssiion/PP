package com.example.pp.config;



import com.example.pp.dto.TourPoiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
public class TourApiV2Client {
    private final WebClient wc;
    private final String serviceKey;
    private final String mobileApp;
    private static final Logger log = LoggerFactory.getLogger(TourApiV2Client.class);
    public TourApiV2Client(
    @Qualifier("tourWebClient") WebClient wc,
    @Value("${tour.api.service-key}")  String serviceKey,
    @Value("${tour.api.mobile-app:pp-app}")  String mobileApp){
        this.wc = wc;
        this.serviceKey = serviceKey;
        this.mobileApp = mobileApp;
    }


    public Mono<TourPoiResponse> locationBasedList2(double mapX, double mapY, int radiusMeters,
                                                    Integer pageNo, Integer numOfRows, String arrange, String type) {
        log.info("[TourAPI 요청] 위치기반: 경도(X)={}, 위도(Y)={}, 반경(m)={}, 페이지={}, 행수={}, 정렬={}",
                mapX, mapY, radiusMeters, pageNo, numOfRows, arrange);
        return wc.get().uri(b -> b
                        .path("/locationBasedList2")
                        .queryParam("serviceKey", "rUA%2FIQ7GuIQ3GK8uD%2F9BxWRIuMpc6g5%2B5ou8WDjh7UqRTnnG0cetRc3KXKV1E3kCaj5I6xEkKdJuqf09MJ0nWA%3D%3D")
                        .queryParam("MobileOS", "WEB")
                        .queryParam("MobileApp", "mobileApp")
                        .queryParam("_type", "json")
                        .queryParam("mapX", mapX)   // 경도
                        .queryParam("mapY", mapY)   // 위도
                        .queryParam("radius", radiusMeters)
                        .queryParam("pageNo", pageNo==null?1:pageNo)
                        .queryParam("numOfRows", numOfRows==null?10000:numOfRows)
                        .queryParam("arrange", arrange==null?"C":arrange) // C=수정일순
                        .queryParam("contentTypeId", type)
                        .build())
                .retrieve()
                .bodyToMono(TourPoiResponse.class)
                .doOnError(e -> log.error("[TourAPI 오류] 위치기반 실패: {}", e.toString(), e)) // 오류 로그 [web:77]
                .doOnSuccess(r -> {
                    int items = Optional.ofNullable(r).map(TourPoiResponse::response)
                            .map(TourPoiResponse.Resp::body)
                            .map(TourPoiResponse.Body::items)
                            .map(TourPoiResponse.Items::item)
                            .map(List::size).orElse(0);
                    log.info("[TourAPI 응답] 위치기반 반환 건수={}", items); // 결과 로그 [web:27]
                });
    }
    // 지역기반(서울=1) 페이지 수집
    public Mono<TourPoiResponse> areaBasedList2Seoul() {
        return wc.get().uri(b -> b
                        .path("/areaBasedList2")
                        .queryParam("MobileOS", "WEB")
                        .queryParam("MobileApp", "mobileApp")
                        .queryParam("_type", "json")
                        .queryParam("serviceKey", "rUA%2FIQ7GuIQ3GK8uD%2F9BxWRIuMpc6g5%2B5ou8WDjh7UqRTnnG0cetRc3KXKV1E3kCaj5I6xEkKdJuqf09MJ0nWA%3D%3D") // 실제 운영 시 serviceKey로 교체
                        .queryParam("areaCode", "1")
                        .queryParam("pageNo", "1")
                        .queryParam("numOfRows", 10000)
                        .build())
                .retrieve()
                .bodyToMono(TourPoiResponse.class)
                .doOnError(e -> log.error("[TourAPI 오류] 지역기반(서울) 실패: {}", e.toString(), e))
                .doOnSuccess(r -> {
                    Integer cnt = Optional.ofNullable(r).map(TourPoiResponse::response)
                            .map(TourPoiResponse.Resp::body)
                            .map(TourPoiResponse.Body::totalCount)
                            .orElse(null);
                    log.info("[TourAPI 응답] 지역기반(서울) totalCount={}", cnt);
                });
    }
}
