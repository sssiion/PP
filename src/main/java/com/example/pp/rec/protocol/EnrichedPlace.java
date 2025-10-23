package com.example.pp.rec.protocol;


import com.example.pp.rec.dto.Place;

// AI에게 최종 추천을 맡기기 전,
// 서버가 장소 정보 + 리뷰 요약까지 모두 '강화(Enrich)'한 DTO입니다.
public class EnrichedPlace {

    // 1. 기본 장소 정보
    private Place place;

    // 2. AI가 summarize_external_data.txt로 요약한 리뷰/블로그
    private String aiReviewSummary;

    // 생성자, Getters, Setters
    public EnrichedPlace(Place place, String aiReviewSummary) {
        this.place = place;
        this.aiReviewSummary = aiReviewSummary;
    }

    public Place getPlace() { return place; }
    public void setPlace(Place place) { this.place = place; }
    public String getAiReviewSummary() { return aiReviewSummary; }
    public void setAiReviewSummary(String aiReviewSummary) { this.aiReviewSummary = aiReviewSummary; }
}