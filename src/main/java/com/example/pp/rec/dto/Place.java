package com.example.pp.rec.dto;


import lombok.Getter;
import lombok.Setter;

// DB에서 가져올 장소 정보 DTO
@Getter
@Setter
public class Place {
    private String name;
    private String address;
    private String category;
    private double latitude;
    private double longitude;

    // (임시 Mock용 생성자)
    public Place(String name, String address, String category, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}