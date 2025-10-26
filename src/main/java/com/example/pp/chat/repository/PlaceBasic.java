package com.example.pp.chat.repository;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlaceBasic {
    private String id;       // kakao id or generated
    private String name;     // place_name
    private double lat;      // y
    private double lon;      // x
    private String address;  // road_address_name or address_name
    private double distanceKm;
}