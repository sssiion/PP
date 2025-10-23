package com.example.pp.rec.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PythonCongestionResponse {
    private String datetime;
    private List<String> congestion_levels;
}
