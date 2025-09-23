package com.example.pp.service.list1;

import com.example.pp.dto.LineStationsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class list1_models {
    public static final Logger log = LoggerFactory.getLogger("List1");

    // 좌표 시드(관광지 검색 입력)
    public record Seed(double lon, double lat) {}

    // 레포지토리 좌표 결과
    public record Coord(double lon, double lat) {}

    // 라인 컨텍스트: 정차역 목록 + 정규화 이름→인덱스
    public record LineCtx(List<LineStationsResponse.Row> stops, java.util.Map<String,Integer> nameToIdx) {}

    // DB선+API 매핑 결과: 라인 파라미터("01"), 사람친화 표기("1호선"), 기준 역명(보정 포함)
    // after: train_data의 근거 라인(lineCode)을 포함
    public record FoundLine(String lineCode, String lineParam, String humanLineName, String stationNameKo) {}

    // "01" → "1호선"
    // list1_models
    public static String toHumanLine(String lineParam) {
        if (lineParam == null) return "";
        String s = lineParam.trim();
        if (s.matches("\\d+")) {           // <-- 수정
            s = s.replaceFirst("^0+(?!$)", "");
            return s + "호선";
        }
        return s;
    }

    // 역명 접미사 보정("역"이 없으면 붙이기)
    public static String ensureStationSuffix(String name) {
        if (name == null) return "";
        String n = name.trim();
        return n.endsWith("역") ? n : n + "역";
    }

    // 이름 정규화(괄호/연속공백 제거)
    public static String normalizeName(String name) {
        if (name == null) return "";
        return name.replace("(", " ")
                .replace(")", " ")
                .replaceAll("\\s+", " ") // <-- 수정
                .trim();
    }

}
