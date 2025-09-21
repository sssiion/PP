package com.example.pp.config;



import com.example.pp.dto.TourPoiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class TourApiV2Client {
    private final WebClient wc;
    private final String serviceKey;
    private final String mobileApp;

    public TourApiV2Client(
    @Qualifier("tourWebClient") WebClient wc,
    @Value("${tour.api.service-key}")  String serviceKey,
    @Value("${tour.api.mobile-app:pp-app}")  String mobileApp){
        this.wc = wc;
        this.serviceKey = serviceKey;
        this.mobileApp = mobileApp;
    }


    public Mono<TourPoiResponse> locationBasedList2(double mapX, double mapY, int radiusMeters,
                                                    Integer pageNo, Integer numOfRows, String arrange) {
        return wc.get().uri(b -> b
                        .path("/locationBasedList2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", "WEB")
                        .queryParam("MobileApp", mobileApp)
                        .queryParam("_type", "json")
                        .queryParam("mapX", mapX)   // 경도
                        .queryParam("mapY", mapY)   // 위도
                        .queryParam("radius", radiusMeters)
                        .queryParam("pageNo", pageNo==null?1:pageNo)
                        .queryParam("numOfRows", numOfRows==null?10000:numOfRows)
                        .queryParam("arrange", arrange==null?"C":arrange) // C=수정일순
                        .build())
                .retrieve()
                .bodyToMono(TourPoiResponse.class);
    }
    // 지역기반(서울=1) 페이지 수집
    public Mono<TourPoiResponse> areaBasedList2Seoul() {
        return wc.get().uri(b -> b
                        .path("/areaBasedList2")
                        .queryParam("MobileOS", "WEB")
                        .queryParam("MobileApp", "mobileApp")
                        .queryParam("_type", "json")
                        .queryParam("serviceKey", "726a7271427274613130356952534a4e") // 실제 운영 시 serviceKey로 교체
                        .queryParam("areaCode", "1")
                        .queryParam("pageNo", "1")
                        .queryParam("numOfRows", "10000")
                        .build())
                .retrieve()
                .bodyToMono(TourPoiResponse.class);
    }
}
