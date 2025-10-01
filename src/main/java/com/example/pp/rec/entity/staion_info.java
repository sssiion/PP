package com.example.pp.rec.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name="station_info")
public class staion_info {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "station_id", nullable = false, length = 50,updatable = false)
    private String stationId;              // 역번

    @Column(name = "station_name", nullable = false, length = 100,updatable = false)
    private String stationName;            // 역사명

    @Column(name = "line_number", nullable = false, length = 50,updatable = false)
    private String lineName;               // 노선명

    @Column(name = "latitude", nullable = false,updatable = false)
    private double lat;                    // 위도

    @Column(name = "longitude", nullable = false,updatable = false)
    private double lon;                    // 경도



    public staion_info(String stationId, String stationName,
                       String lineName, double lat, double lon) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.lineName = lineName;
        this.lat = lat;
        this.lon = lon;
    }
}
