package com.example.pp.config;


import com.example.pp.dto.*;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class SeoulMetroClient {

    private final WebClient wc;
    private final String apiKey;

    public SeoulMetroClient(
            @Qualifier("seoulMetroWebClient") WebClient wc,
            @Value("${seoulmetro.api.key}") String apiKey
    ) {
        this.wc = wc;
        this.apiKey = apiKey;
    }
    private static final ParameterizedTypeReference<SeoulWrap<StationByName.Item>> TYPE_BY_NAME =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<SeoulWrap<TimeTableByCode.Item>> TYPE_TT =
            new ParameterizedTypeReference<>() {};

    // 1) 역명 → 역코드/외부코드/호선 (JSON)
    public Mono<SeoulWrap<StationByName.Item>> searchByStationName(String stationName) {
        return wc.get()
                .uri(b -> b.pathSegment(apiKey, "json", "SearchInfoBySubwayNameService", "1", "100", stationName).build())
                .retrieve()
                .bodyToMono(TYPE_BY_NAME);
    }

    // 2) 역코드 → 시간표(요일 1/2/3, 방면 0/1)
    public Mono<SeoulWrap<TimeTableByCode.Item>> timeTableByStationCode(String stationCode,
                                                                        String dayType, String upDown) {
        return wc.get()
                .uri(b -> b.pathSegment(apiKey, "json", "SearchSTNTimeTableByIDService", "1", "500",
                        stationCode, dayType, upDown).build())
                .retrieve()
                .bodyToMono(TYPE_TT);
    }

    // 3) 노선번호/명 → 정차역 목록 (JSON) — 1~1000 페이징 지원
    public Mono<LineStationsResponse> fetchLineStations(String lineParam, int start, int end) {
        return wc.get()
                .uri(b -> b.pathSegment(apiKey, "json", "SearchSTNBySubwayLineInfo",
                        String.valueOf(start), String.valueOf(end), lineParam).build())
                .retrieve()
                .bodyToMono(LineStationsResponse.class);
    }

    public Mono<LineStationsResponse> fetchLineStationsMax(String lineParam) {
        return fetchLineStations(lineParam, 1, 1000);
    }
}
