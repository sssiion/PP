package com.example.pp.rec.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true) // 모르는 필드는 무시
public class PredefinedPlace {
    private String name;
    private String id;
    private String address;
    private String longitude;
    private String latitude;

    @JsonProperty("other_info") // JSON 필드명과 Java 변수명 매핑
    private String otherInfo;

    private String category; // (카테고리 ID가 문자열이므로 String)

    @JsonProperty("postal_code")
    private String postalCode;

    // (JsonProperty 추가)
    @JsonProperty("congestionLevel")
    private String congestionLevel;
}