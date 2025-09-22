package com.example.pp.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StopItem(
        @JsonProperty("citycode") String cityCode,
        @JsonProperty("nodeid")   String nodeId,
        @JsonProperty("nodenm")   String nodeNm,
        @JsonProperty("gpslati")  Double gpsLati,
        @JsonProperty("gpslong")  Double gpsLong
) {}
