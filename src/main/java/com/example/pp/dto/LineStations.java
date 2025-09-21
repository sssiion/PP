package com.example.pp.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface LineStations {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(
            @JsonProperty("STATION_CD") String stationCode,
            @JsonProperty("STATION_NM") String stationNameKo,
            @JsonProperty("STATION_NM_ENG") String stationNameEn,
            @JsonProperty("STATION_NM_CHN") String stationNameZh,
            @JsonProperty("STATION_NM_JPN") String stationNameJp,
            @JsonProperty("LINE_NUM") String lineNum,
            @JsonProperty("FR_CODE") String outerCode
    ) {}
}
