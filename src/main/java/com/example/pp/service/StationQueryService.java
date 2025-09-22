package com.example.pp.service;

import com.example.pp.entity.staion_info;
import com.example.pp.repository.SubwayStationDistanceView;
import com.example.pp.repository.SubwayStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationQueryService {
    private final SubwayStationRepository repo;
    private static final double FIXED_RADIUS_M = 50.0;

    // 엔티티 그대로 반환(반경 50m 고정)
    public List<staion_info> within50m(double lat, double lon) {
        return repo.findAllWithinRadiusOrderByDistanceAsc(lat, lon, FIXED_RADIUS_M);
    }

    // 거리 포함 프로젝션(반경 50m 고정)
    public List<SubwayStationDistanceView> within50mWithDistance(double lat, double lon) {
        return repo.findAllWithinRadiusWithDistanceOrderByDistanceAsc(lat, lon, FIXED_RADIUS_M);
    }
}
