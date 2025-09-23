package com.example.pp.service.list1;


import com.example.pp.entity.staion_info;
import com.example.pp.repository.SubwayStationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class list1_nearby_stations {
    // 주변역 조회 서비스(동기 JPA)
    private static final Logger log = LoggerFactory.getLogger(list1_nearby_stations.class);
    private final SubwayStationRepository stationRepo;

    public list1_nearby_stations(SubwayStationRepository stationRepo) {
        this.stationRepo = stationRepo;
    }

    // // 입력: 사용자 위/경도
    // // 처리: 100m 반경 내 역을 거리순으로 조회
    // // 출력: staion_info 리스트
    public List<staion_info> fetch(double lat, double lon) {
        List<staion_info> nears = stationRepo.findAllWithinRadiusOrderByDistanceAsc(lat, lon);
        log.info("[List1] nearby stations(100m) count={}", nears.size());

        return nears;
    }
}
