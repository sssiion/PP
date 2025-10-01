package com.example.pp.rec.service.list1;

import com.example.pp.rec.config.SeoulMetroClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class list1_code_param_mapper {
    // 역명 1회 호출로 stationCode(=lineCode) → lineParam("01") 매핑 생성
    private final SeoulMetroClient seoul;

    public list1_code_param_mapper(SeoulMetroClient seoul) { this.seoul = seoul; }

    // // 입력: 역명(접미사 보정 완료)
    // // 처리: API 역명 검색 → stationCode, lineNum 추출
    // // 출력: Map<stationCode, lineParam>
    public Mono<Map<String,String>> codeToParamByStation(String stationNameEnsured) {
        return seoul.searchByStationName(stationNameEnsured)
                .flatMapMany(Flux::fromIterable)
                .collect(Collectors.toMap(
                        SeoulMetroClient.StationSimpleDto::stationCode,
                        SeoulMetroClient.StationSimpleDto::lineNum,
                        (a,b)->a,
                        LinkedHashMap::new
                ));
    }
}
