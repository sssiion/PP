package com.example.pp.rec.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BusStop {

    @Id
    @Column(name = "station_no")
    private Integer stationNo; // 정류장 번호 (기본 키)

    @Column(name = "station_name", nullable = false)
    private String stationName; // 정류장 이름

    @Column(nullable = false)
    private Double latitude; // 위도

    @Column(nullable = false)
    private Double longitude; // 경도
}