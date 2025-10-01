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
public class BusRoute {

    @Id
    @Column(name = "route_id")
    private String routeId; // 노선 ID (예: GS05_DOWN, 기본 키)

    @Column(name = "route_no", nullable = false)
    private String routeNo; // 노선 번호 (예: 강서05)

    @Column(name = "start_stop_name", nullable = false)
    private String startStopName; // 기점

    @Column(name = "end_stop_name", nullable = false)
    private String endStopName; // 종점
}