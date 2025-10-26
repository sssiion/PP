package com.example.pp.chat.repository;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlaceWithCongestion {
    private String id;
    private String name;
    private double lat;
    private double lon;
    private String address;
    private double distanceKm;
    private String congestionLevel; // 여유/보통/약간 붐빔/붐빔/정보없음
}