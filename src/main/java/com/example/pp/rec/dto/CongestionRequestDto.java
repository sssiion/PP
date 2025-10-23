package com.example.pp.rec.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CongestionRequestDto {
    private Double latitude;
    private Double longitude;
    private String datetime;
}
