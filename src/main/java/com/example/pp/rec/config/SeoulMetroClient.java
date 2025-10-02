package com.example.pp.rec.config;





import com.example.pp.rec.dto.LineStationsResponse;
import com.example.pp.rec.dto.SeoulWrap;
import com.example.pp.rec.dto.StationByName;
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
                .uri(b -> b.pathSegment(apiKey, "json", "SearchInfoBySubwayNameService", "1", "1000","/", stationName).build())
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

    // 단일 페이지 호출: start/end를 pathSegment로 분리해 전달
    public Mono<LineStationsResponse> fetchLineStationsPage(String lineParam) {
        log.info("[서울메트로 요청] 노선 정차역: line={}", lineParam);
        return wc.get()
                .uri(b -> b.path("/"+
                        apiKey+ "/json"+ "/SearchSTNBySubwayLineInfo"+
                        "/1"+ "/1000"+ "/%20/%20/"+lineParam
                ).build())
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




    // 1~1000 구간만 호출하여 Row 리스트 반환
    public Mono<List<LineStationsResponse.Row>> fetchLineStations1k(String lineParam) {
        return fetchLineStationsPage(lineParam)
                .map(resp -> Optional.ofNullable(resp.service())
                        .map(LineStationsResponse.ServiceBlock::rows)
                        .orElseGet(List::of));
    }
}
