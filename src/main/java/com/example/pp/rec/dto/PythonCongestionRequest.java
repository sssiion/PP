package com.example.pp.rec.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PythonCongestionRequest {
    private String datetime;
    private List<LocationDto> locations;
}
