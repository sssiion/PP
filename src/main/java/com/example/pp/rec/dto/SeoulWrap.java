package com.example.pp.rec.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeoulWrap<T>(
        Result<T> SearchInfoBySubwayNameService,
        Result<T> SearchSTNTimeTableByIDService,
        Result<T> SearchSTNBySubwayLineInfo
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Result<T>(Integer list_total_count, ResultMsg RESULT, List<T> row) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record ResultMsg(String CODE, String MESSAGE) {}
}
