package com.example.pp.chat.dto;


import lombok.Data;
import java.util.*;
@Data
public class ParsedIntent {
    private String placeName;
    private String placeType;
    private Double lat, lon;
    private Integer radius; // 미터
    private String address;
    private List<String> keywords;
    private boolean ready; // API 호출 조건이 모두 갖춰졌는지
    private String askWhatIsMissing; // 부족시 추가질문

    public boolean isReady() { return ready; }
}
