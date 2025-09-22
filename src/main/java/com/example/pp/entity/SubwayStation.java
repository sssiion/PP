package com.example.pp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "station_info")
@Getter
@NoArgsConstructor
public class SubwayStation {

    @Id
    @Column(name = "station_id", nullable = false, length = 50)
    private String stationId;              // 역번

    @Column(name = "station_name", nullable = false, length = 100)
    private String stationName;            // 역사명

    @Column(name = "line_number", nullable = false, length = 50)
    private String lineName;               // 노선명

    @Column(name = "latitude", nullable = false)
    private double lat;                    // 위도

    @Column(name = "longitude", nullable = false)
    private double lon;                    // 경도



    public SubwayStation(String stationId, String stationName,
                         String lineName, double lat, double lon) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.lineName = lineName;
        this.lat = lat;
        this.lon = lon;
    }
}
