package com.example.pp.chat.dto;


import lombok.Data;
import java.util.*;
@Data
public class ParsedIntent {
    private String placeName;
    private String placeType;
    private Double lat, lon;
    private Integer radius; // 반경(미터)
    private String address;
    private List<String> keywords = new ArrayList<>();
    private boolean ready;
    private String askWhatIsMissing;

    public boolean isReady() {
        return ready;
    }
}