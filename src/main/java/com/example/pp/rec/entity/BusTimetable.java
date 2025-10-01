package com.example.pp.rec.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor
public class BusTimetable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private BusRoute busRoute; // 노선 (FK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_no")
    private BusStop busStop; // 정류장 (FK)

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime; // 도착 시각

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false)
    private DayType dayType; // 운행 요일 (평일/주말)

    public enum DayType {
        WEEKDAY, WEEKEND
    }

    public BusTimetable(BusRoute busRoute, BusStop busStop, LocalTime arrivalTime, DayType dayType) {
        this.busRoute = busRoute;
        this.busStop = busStop;
        this.arrivalTime = arrivalTime;
        this.dayType = dayType;
    }
}