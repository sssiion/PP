package com.example.pp.rec.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FinalRecommendationResponse {
    private String recommendation; // "A 장소를 추천합니다..."
    private List<String> pros; // ["접근성이 좋습니다", "..."]
    private List<String> cons; // ["가격대가 있습니다"]
}
