package com.example.pp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "train_data")
@Getter @NoArgsConstructor
public class TrainSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_code", nullable = false, length = 20)   // 예: "150"
    private String lineCode;

    @Column(name = "station_name", nullable = false, length = 100)
    private String stationName;                                  // 예: "서울역"

    @Column(name = "weekday_weekend", nullable = false, length = 10)    // 예: "DAY"
    private String dayType;

    @Column(name = "direction", nullable = false, length = 10)   // 예: "DOWN"
    private String direction;

    @Column(name = "is_express", nullable = false)
    private int expressFlag;                                     // 0: 일반

    @Column(name = "train_code", nullable = false, length = 20)    // 예: "K101"
    private String trainNo;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrTime;                                   // TIME 도착

    @Column(name = "departure_time", nullable = false)
    private LocalTime depTime;                                   // TIME

    @Column(name = "arrival_name", length = 100)                    // 예: "인천"
    private String destName;

    @Column(name = "departure_name", length = 100)                    // 예: "양주"
    private String origName;
}