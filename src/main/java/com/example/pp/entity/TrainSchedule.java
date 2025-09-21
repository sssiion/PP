package com.example.pp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "train_schedule",
        indexes = {
                @Index(name = "idx_ts_station_day", columnList = "station_name, day_type"),
                @Index(name = "idx_ts_arr_time", columnList = "arr_time")
        })
@Getter @NoArgsConstructor
public class TrainSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "line_code", nullable = false, length = 20)   // 예: "150"
    private String lineCode;

    @Column(name = "station_name", nullable = false, length = 100)
    private String stationName;                                  // 예: "서울역"

    @Column(name = "day_type", nullable = false, length = 10)    // 예: "DAY"
    private String dayType;

    @Column(name = "direction", nullable = false, length = 10)   // 예: "DOWN"
    private String direction;

    @Column(name = "express_flag", nullable = false)
    private int expressFlag;                                     // 0: 일반

    @Column(name = "train_no", nullable = false, length = 20)    // 예: "K101"
    private String trainNo;

    @Column(name = "arr_time", nullable = false)
    private LocalTime arrTime;                                   // TIME

    @Column(name = "dep_time", nullable = false)
    private LocalTime depTime;                                   // TIME

    @Column(name = "dest_name", length = 100)                    // 예: "인천"
    private String destName;

    @Column(name = "orig_name", length = 100)                    // 예: "양주"
    private String origName;
}