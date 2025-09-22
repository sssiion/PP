package com.example.pp.repository;

public interface SubwayStationDistanceView {
    String getStationId();
    String getStationName();
    String getLineNo();
    String getLineName();
    Double getLat();
    Double getLon();

    String getRoadAddress();
    Double getDistance(); // meters
}
