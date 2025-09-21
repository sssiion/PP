package com.example.pp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subway_station",
        indexes = {
                @Index(name = "idx_subway_station_line_no", columnList = "line_no"),
                @Index(name = "idx_subway_station_lat_lon", columnList = "lat, lon")
        })
@Getter
@NoArgsConstructor
public class SubwayStation {

    @Id
    @Column(name = "station_id", nullable = false, length = 50)
    private String stationId;              // 역번

    @Column(name = "station_name", nullable = false, length = 100)
    private String stationName;            // 역사명

    @Column(name = "line_no", nullable = false, length = 20)
    private String lineNo;                 // 노선번호

    @Column(name = "line_name", nullable = false, length = 50)
    private String lineName;               // 노선명

    @Column(name = "lat", nullable = false)
    private double lat;                    // 위도

    @Column(name = "lon", nullable = false)
    private double lon;                    // 경도

    @Column(name = "road_address", length = 200)
    private String roadAddress;            // 도로명주소

    public SubwayStation(String stationId, String stationName, String lineNo,
                         String lineName, double lat, double lon, String roadAddress) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.lineNo = lineNo;
        this.lineName = lineName;
        this.lat = lat;
        this.lon = lon;
        this.roadAddress = roadAddress;
    }
}
