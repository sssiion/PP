package com.example.pp.config;


import com.example.pp.dto.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
public class SeoulMetroClient {

    private final WebClient wc;
    private final String apiKey;
    private static final Logger log = LoggerFactory.getLogger(SeoulMetroClient.class);
    public SeoulMetroClient(
            @Qualifier("seoulMetroWebClient") WebClient wc,
            @Value("${seoulmetro.api.key}") String apiKey
    ) {
        this.wc = wc;
        this.apiKey = apiKey;
    }
    private static final ParameterizedTypeReference<SeoulWrap<StationByName.Item>> TYPE_BY_NAME =
            new ParameterizedTypeReference<>() {};


    // 1) 역명 → 역코드/외부코드/호선 (JSON)
    public Mono<SeoulWrap<StationByName.Item>> searchByStationName(String stationName) {
        log.info("[서울메트로 요청] 역명 검색: {}", stationName);
        return wc.get()
                .uri(b -> b.path("/" +apiKey+ "/json"+ "/SearchInfoBySubwayNameService"+"/1"+ "/100/"+ stationName+"/").build())
                .retrieve()
                .bodyToMono(TYPE_BY_NAME)
                .doOnError(e -> log.error("[서울메트로 오류] 역명 검색 실패: {}", e.toString(), e)) // 오류 로그 [web:77]
                .doOnSuccess(w -> {
                    int rows = Optional.ofNullable(w.SearchInfoBySubwayNameService())
                            .map(SeoulWrap.Result::row).map(List::size).orElse(0);
                    log.info("[서울메트로 응답] 역명 검색 결과 행 수={}", rows); // 결과 로그 [web:26]
                });
    }

    // 2) 역코드 → 시간표(요일 1/2/3, 방면 0/1)
    //public Mono<SeoulWrap<TimeTableByCode.Item>> timeTableByStationCode(String stationCode,
      //                                                                  String dayType, String upDown) {
      //  return wc.get()
      //          .uri(b -> b.pathSegment(apiKey, "json", "SearchSTNTimeTableByIDService", "1", "500",
      //                  stationCode, dayType, upDown).build())
      //          .retrieve()
      //          .bodyToMono(TYPE_TT);
   // }

    // 3) 노선번호/명 → 정차역 목록 (JSON) — 1~1000 페이징 지원
    public Mono<LineStationsResponse> fetchLineStations(String lineParam) {
        log.info("[서울메트로 요청] 노선 정차역: line={} start={} end={}", lineParam);
        return wc.get()
                .uri(b -> b.path("/" +apiKey+ "/json"+ "/SearchSTNBySubwayLineInfo"+"/1"+ "/100"+"/"+ lineParam).build())
                .retrieve()
                .bodyToMono(LineStationsResponse.class)
                .doOnError(e -> log.error("[서울메트로 오류] 노선 정차역 실패: {}", e.toString(), e)) // 오류 로그 [web:77]
                .doOnSuccess(r -> {
                    int rows = Optional.ofNullable(r.service()).map(LineStationsResponse.ServiceBlock::rows)
                            .map(List::size).orElse(0);
                    log.info("[서울메트로 응답] 노선 정차역 행 수={}", rows); // 결과 로그 [web:21]
                });
    }

    public Mono<LineStationsResponse> fetchLineStationsMax(String lineParam) {
        return fetchLineStations(lineParam);
    }
}
