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
import java.util.stream.Collectors;
@Component
public class SeoulMetroClient {

    private final WebClient wc;
    private final String apiKey;
    private static final Logger log = LoggerFactory.getLogger(SeoulMetroClient.class);

    public SeoulMetroClient(@Qualifier("seoulMetroWebClient") WebClient wc,
                            @Value("${seoulmetro.api.key}") String apiKey) {
        this.wc = wc;
        this.apiKey = apiKey;
    }

    private static final ParameterizedTypeReference<SeoulWrap<StationByName.Item>> TYPE_BY_NAME =
            new ParameterizedTypeReference<>() {};

    // 1) 역명 → DTO 리스트
    public Mono<List<StationSimpleDto>> searchByStationName(String stationName) {
        log.info("[서울메트로 요청] 역명 검색: {}", stationName);
        return wc.get()
                .uri(b -> b.pathSegment(apiKey, "json", "SearchInfoBySubwayNameService", "1", "100", stationName).build())
                .retrieve()
                .bodyToMono(TYPE_BY_NAME)
                .map(w -> Optional.ofNullable(w.SearchInfoBySubwayNameService())
                        .map(SeoulWrap.Result::row)
                        .orElseGet(List::of))
                .map(rows -> rows.stream()
                        .map(r -> new StationSimpleDto(
                                r.stationCode(), r.lineNum(), r.stationName(), r.outerCode()
                        ))
                        .collect(Collectors.toList()))
                .doOnError(e -> log.error("[서울메트로 오류] 역명 검색 실패(simple): {}", e.toString(), e))
                .doOnSuccess(list -> log.info("[서울메트로 응답] simple 행 수={}", list.size()));
    }

    public record StationSimpleDto(String stationCode, String lineNum, String stationNameKo, String outerCode) {}

    // 3) 노선번호/명 → 정차역 목록 (1~1000)
    public Mono<LineStationsResponse> fetchLineStations(String lineParam) {
        log.info("[서울메트로 요청] 노선 정차역: line={}", lineParam);
        return wc.get()
                .uri(b -> b.pathSegment(apiKey, "json", "SearchSTNBySubwayLineInfo", "1", "1000", lineParam).build())
                .retrieve()
                .bodyToMono(LineStationsResponse.class)
                .doOnError(e -> log.error("[서울메트로 오류] 노선 정차역 실패: {}", e.toString(), e))
                .doOnSuccess(r -> {
                    int rows = Optional.ofNullable(r.service())
                            .map(LineStationsResponse.ServiceBlock::rows)
                            .map(List::size).orElse(0);
                    log.info("[서울메트로 응답] 노선 정차역 행 수={}", rows);
                });
    }

    public Mono<LineStationsResponse> fetchLineStationsMax(String lineParam) {
        return fetchLineStations(lineParam);
    }
}
