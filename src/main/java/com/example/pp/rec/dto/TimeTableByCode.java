package com.example.pp.rec.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface TimeTableByCode {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(
            @JsonProperty("STATION_CD") String stationCode,
            @JsonProperty("ARRIVETIME") String arriveTime,     // HH:mm:ss
            @JsonProperty("LEFTTIME") String departureTime,    // HH:mm:ss
            @JsonProperty("UPDOWN") String upDown,             // 0/1 or 상/하
            @JsonProperty("WEEK_TAG") String dayType,          // 1/2/3
            @JsonProperty("SUBWAYENAME") String destName,      // 목적지 영문명 등
            @JsonProperty("LINE_NUM") String lineNum
    ) {}
}
