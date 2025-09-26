package com.example.pp.repository;


import com.example.pp.entity.BusStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BusStopRepository extends JpaRepository<BusStop, Integer> {

    /**
     * 특정 좌표(lat, lon)를 기준으로 지정된 반경(radius) 내의 모든 버스 정류장을 조회합니다.
     * MySQL의 ST_Distance_Sphere 함수를 사용하여 두 점 사이의 거리를 미터(m) 단위로 계산합니다.
     * @param lat 위도
     * @param lon 경도
     * @param radius 검색할 반경 (미터 단위)
     * @return 반경 내에 있는 BusStop 리스트
     */
    @Query(value = "SELECT * FROM bus_stop s " +
            "WHERE ST_Distance_Sphere(point(s.longitude, s.latitude), point(:lon, :lat)) <= :radius",
            nativeQuery = true)
    List<BusStop> findBusStopsWithinRadius(@Param("lat") double lat, @Param("lon") double lon, @Param("radius") int radius);
}