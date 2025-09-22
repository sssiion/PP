package com.example.pp.config;


import com.example.pp.dto.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    public Mono<LineStationsResponse> fetchLineStationsPage(String lineParam) {
        log.info("[서울메트로 요청] 노선 정차역: line={}", lineParam);
        return wc.get()
                .uri(b -> b.pathSegment(apiKey, "json", "SearchSTNBySubwayLineInfo", "5", "1000","//", lineParam).build())
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


    // 최대 5페이지 × 1000행 = 5000행 병합 반환 (Row 리스트)
    public Mono<List<LineStationsResponse.Row>> fetchLineStationsMax(String lineParam) {
        // 1~1000, 1001~2000, 2001~3000, 3001~4000, 4001~5000
        int[][] ranges = new int[][]{
                {1, 1000}, {1001, 2000}, {2001, 3000}, {3001, 4000}, {4001, 5000}
        };
        return Flux.fromArray(ranges)
                // 순서 보장 위해 concatMap 사용
                .concatMap(r -> fetchLineStationsPage(lineParam)) // [web:230][web:232]
                .map(this::extractRows)
                .reduce(new LinkedHashMap<String, LineStationsResponse.Row>(), (acc, rows) -> {
                    // 역코드 기준 중복 제거, 최초 등장 순서 유지
                    for (var row : rows) acc.putIfAbsent(row.stationCode(), row);
                    return acc;
                })
                .map(m -> new ArrayList<>(m.values()));
    }

    private List<LineStationsResponse.Row> extractRows(LineStationsResponse resp) {
        return Optional.ofNullable(resp.service())
                .map(LineStationsResponse.ServiceBlock::rows)
                .orElseGet(List::of); // [web:11]
    }
}
