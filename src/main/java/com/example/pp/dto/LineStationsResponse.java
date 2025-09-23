package com.example.pp.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LineStationsResponse(
        @JsonProperty("SearchSTNBySubwayLineInfo") ServiceBlock service
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record ServiceBlock(
            @JsonProperty("list_total_count") Integer totalCount,
            @JsonProperty("RESULT") Result result,
            @JsonProperty("row") List<Row> rows
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Result(
            @JsonProperty("CODE") String code,
            @JsonProperty("MESSAGE") String message
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Row(
            @JsonProperty("STATION_CD") String stationCode,     // 예: 1722
            @JsonProperty("STATION_NM") String stationNameKo,   // 예: 서정리
            @JsonProperty("STATION_NM_ENG") String stationNameEn, // Seojeong-ri
            @JsonProperty("LINE_NUM") String lineNum,           // 예: 01호선
            @JsonProperty("FR_CODE") String outerCode,          // 예: P163
            @JsonProperty("STATION_NM_CHN") String stationNameZh, // 西井里
            @JsonProperty("STATION_NM_JPN") String stationNameJp  // ソジョンニ
    ) {}
    // FR_CODE 자연 정렬 + STATION_CD 숫자 정렬
    private static final java.util.regex.Pattern P = java.util.regex.Pattern.compile("^([A-Za-z]*)(\\d+)?");
    public static void sortRowsInPlace(List<Row> rows) {
        rows.sort((a, b) -> {
            SortKey ka = new SortKey(a.outerCode(), a.stationCode());
            SortKey kb = new SortKey(b.outerCode(), b.stationCode());
            return ka.compareTo(kb);
        });
    }
    private static class SortKey implements Comparable<SortKey> {
        final String prefix;   // FR_CODE 알파벳 접두 (없으면 빈 문자열)
        final long num;        // FR_CODE 숫자부 (없으면 +INF 대체)
        final long stationCd;  // STATION_CD 숫자 값 (없으면 +INF 대체)

        SortKey(String frCode, String stationCd) {
            String f = frCode == null ? "" : frCode.trim();
            java.util.regex.Matcher m = P.matcher(f);
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
            try { sd = Long.parseLong(sc.replaceAll("\\D", "")); } // 비숫자 제거 후 파싱
            catch (Exception e) { sd = Long.MAX_VALUE; }
            this.stationCd = sd;
        }
        @Override public int compareTo(SortKey o) {
            int c = this.prefix.compareToIgnoreCase(o.prefix);
            if (c != 0) return c;
            c = Long.compare(this.num, o.num);
            if (c != 0) return c;
            return Long.compare(this.stationCd, o.stationCd);
        }
        // FR_CODE(outerCode) 자연 정렬 + STATION_CD(stationCode) 숫자 정렬 유틸

    }


}
