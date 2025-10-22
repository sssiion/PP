package com.example.pp.rec.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CongestionRequestDto {
    private String lat;
    private String lon;
    private String datetime;
}
