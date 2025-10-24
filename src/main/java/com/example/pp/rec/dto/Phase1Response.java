package com.example.pp.rec.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Phase1Response {
    private String status; // "collectING_DATA" or "PLACE_QUERY_READY"
    private String reply; // 사용자에게 보여줄 AI 답변
    private Map<String, String> collectedData; // (선택적) 수집된 데이터
}
