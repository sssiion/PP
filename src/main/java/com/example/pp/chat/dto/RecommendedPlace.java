package com.example.pp.chat.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendedPlace {
    private String id;
    private String name;         // Kakao 기준
    private String address;      // Kakao 기준
    private double latitude;
    private double longitude;
    private String category;
    private String congestionLevel;
    private double distance;     // km
    private String originalOtherInfo;
    private PlaceDetails data;
}
