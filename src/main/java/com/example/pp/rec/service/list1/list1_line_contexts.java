package com.example.pp.rec.service.list1;

import com.example.pp.rec.config.SeoulMetroClient;
import com.example.pp.rec.dto.LineStationsResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import static com.example.pp.rec.dto.LineStationsResponse.sortRowsInPlace;
import static com.example.pp.rec.service.list1.list1_models.normalizeName;

@Service
public class list1_line_contexts {
    private final SeoulMetroClient seoul;

    public list1_line_contexts(SeoulMetroClient seoul) { this.seoul = seoul; }

    // FR_CODE 자연 정렬용 (문자 접두 + 숫자부)
    private static final Pattern FR = Pattern.compile("^([A-Za-z]*)(\\d+)?");

    private static final class SortKey implements Comparable<SortKey> {
        final String prefix;    // FR_CODE 문자 접두
        final long num;         // FR_CODE 숫자부(없으면 +INF)
        final long stationNum;  // STATION_CD 숫자부(없으면 +INF)

        SortKey(String frCode, String stationCd) {
            String f = frCode == null ? "" : frCode.trim();
            Matcher m = FR.matcher(f);
            if (m.find()) {
                this.prefix = m.group(1) == null ? "" : m.group(1);
                String n = m.group(2);
                this.num = (n == null || n.isEmpty()) ? Long.MAX_VALUE : Long.parseLong(n);
            } else {
                this.prefix = "";
                this.num = Long.MAX_VALUE;
            }
            String sc = stationCd == null ? "" : stationCd.trim();
            long sd;
            try { sd = Long.parseLong(sc.replaceAll("\\D", "")); }
            catch (Exception e) { sd = Long.MAX_VALUE; }
            this.stationNum = sd;
        }
        @Override public int compareTo(SortKey o) {
            int c = this.prefix.compareToIgnoreCase(o.prefix);
            if (c != 0) return c;
            c = Long.compare(this.num, o.num);
            if (c != 0) return c;
            return Long.compare(this.stationNum, o.stationNum);
        }
    }



    public Mono<Map<String, list1_models.LineCtx>> fetch(Set<String> lineParams) {
        return Flux.fromIterable(lineParams)
                .flatMap(line ->
                        seoul.fetchLineStations1k(line)
                                .map((List<LineStationsResponse.Row> stops) -> {
                                    // 1) 정렬되지 않은 정차역 목록을 FR_CODE → STATION_CD 기준으로 정렬
                                    sortRowsInPlace(stops);

                                    // 2) 정렬된 순서를 기준으로 이름→인덱스 맵 구성
                                    Map<String,Integer> nameToIdx = new LinkedHashMap<>();
                                    for (int i = 0; i < stops.size(); i++) {
                                        nameToIdx.putIfAbsent(normalizeName(stops.get(i).stationNameKo()), i);
                                    }

                                    // 3) 정렬된 정차역과 인덱스 맵을 컨텍스트로 반환
                                    return Map.entry(line, new list1_models.LineCtx(stops, nameToIdx));
                                })
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }
}
