package com.example.pp.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface StationByName {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(
            @JsonProperty("STATION_CD") String stationCode,
            @JsonProperty("FR_CODE") String outerCode,
            @JsonProperty("STATION_NM") String stationName,
            @JsonProperty("LINE_NUM") String lineNum
    ) {}
}
