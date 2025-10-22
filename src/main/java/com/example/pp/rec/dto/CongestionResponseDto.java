package com.example.pp.rec.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CongestionResponseDto {
    private double latitude;
    private double longitude;
    private String datetime;
    @JsonProperty("congestion_level")
    private String congestionLevel;
}
