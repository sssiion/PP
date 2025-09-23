package com.example.pp.service.list1;


import com.example.pp.entity.staion_info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.pp.service.list1.list1_models.ensureStationSuffix;
import static com.example.pp.service.list1.list1_models.toHumanLine;


@Service
public class list1_find_lines {
    // DB 선 + API 매핑 결합으로 FoundLine 생성
    private static final Logger log = LoggerFactory.getLogger(list1_find_lines.class);
    private final list1_db_prefilter db;
    private final list1_code_param_mapper mapper;

    public list1_find_lines(list1_db_prefilter db, list1_code_param_mapper mapper) {
        this.db = db;
        this.mapper = mapper;
    }

    // // 입력: 주변역 리스트, dayType, 시작/종료 시각
    // // 처리: 역명 보정 → DB earliestByLineCode → code→param 매핑 → FoundLine
    // // 출력: List<FoundLine>
    public Mono<List<list1_models.FoundLine>> find(List<staion_info> nears, String dayType, LocalTime start, LocalTime end) {
        return Flux.fromIterable(nears)
                .concatMap(st -> {
                    String ensured = list1_models.ensureStationSuffix(st.getStationName()); // 역명 접미사 보정
                    // DB에서 라인 문자열까지 포함해 가져오도록 쿼리/매핑 확장(traindata에 line 필드 매핑 필요)
                    return db.earliestByTrainLine(ensured, dayType, start, end) // Map<String lineName, LocalTime earliest>
                            .flatMapMany(earliestByLine -> {
                                if (earliestByLine.isEmpty()) return Flux.empty();
                                // 역명 검색 없이, train_data 라인을 파라미터로 변환해 바로 사용
                                List<list1_models.FoundLine> out = new ArrayList<>();
                                earliestByLine.forEach((lineName, firstArr) -> {
                                    String lineParam = toLineParam(lineName); // "1호선" -> "01"
                                    if (lineParam != null) {
                                        out.add(new list1_models.FoundLine(
                                                /* lineCode 불필요하면 제거 가능 */ lineName, // 근거 라인 표기로 보관
                                                lineParam,
                                                lineName,        // 사람이 읽는 라인명은 DB 값 그대로 사용
                                                ensured
                                        ));
                                    }
                                });
                                return Flux.fromIterable(out);
                            });
                })
                .collect(Collectors.toMap(
                        (list1_models.FoundLine f) -> f.lineParam() + "|" + f.stationNameKo(),
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ))
                .map((Map<String, list1_models.FoundLine> m) -> new ArrayList<>(m.values()));
    }

    // 라인 문자열 → 라인 파라미터 변환 유틸(필요 라인만 추가)
    private String toLineParam(String lineName) {
        if (lineName == null) return null;
        String n = lineName.trim().replace("호선", "");
        if (n.matches("\\d+")) return String.format("%02d", Integer.parseInt(n)); // <-- 수정
        return null;
    }
}