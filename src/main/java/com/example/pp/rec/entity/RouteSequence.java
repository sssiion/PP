package com.example.pp.rec.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class RouteSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private BusRoute busRoute; // 노선 (FK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_no")
    private BusStop busStop; // 정류장 (FK)

    @Column(name = "sequence", nullable = false)
    private Integer sequence; // 해당 노선에서의 정류장 순서

    public RouteSequence(BusRoute busRoute, BusStop busStop, Integer sequence) {
        this.busRoute = busRoute;
        this.busStop = busStop;
        this.sequence = sequence;
    }
}