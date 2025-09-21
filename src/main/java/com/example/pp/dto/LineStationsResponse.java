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
}
