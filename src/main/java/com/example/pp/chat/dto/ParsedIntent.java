package com.example.pp.chat.dto;


import lombok.Data;
import java.util.*;

@Data
public class ParsedIntent {
    private String placeName;
    private String placeType;
    private Double lat, lon;
    private String address;
    private List<String> keywords;      // 필터, 조건 등
    private boolean ready;              // 정보 충분 여부
    private String askWhatIsMissing;    // 부족시 추가질문

    public boolean isReady() { return ready; }
    public String askMore() { return askWhatIsMissing==null?"더 구체적으로 부탁드립니다.":askWhatIsMissing; }
}
