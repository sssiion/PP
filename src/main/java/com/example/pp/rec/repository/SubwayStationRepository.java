package com.example.pp.rec.repository;


import com.example.pp.rec.entity.staion_info;
import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;

public interface SubwayStationRepository extends JpaRepository<staion_info, String> {

    // 1000m 반경 내 역을 거리순으로(로그의 ST_Distance_Sphere SQL 기반)
    @Query(value = """
      SELECT s.* 
      FROM station_info s
      WHERE ST_Distance_Sphere(POINT(s.longitude, s.latitude), POINT(?2, ?1)) <= 1000
      ORDER BY ST_Distance_Sphere(POINT(s.longitude, s.latitude), POINT(?2, ?1)) ASC
      """, nativeQuery = true)
    List<staion_info> findAllWithinRadiusOrderByDistanceAsc(double lat, double lon);

    // (라인+역명) 우선 좌표 해석
    @Query("SELECT s FROM staion_info s WHERE s.stationName = :stationName")
    Optional<staion_info> findOneByStationName(String stationName);

    // 아래 3개는 폴백 체인에 사용(스키마에 맞게 구현되어 있지 않다면 제거해도 됨)
    @Query("SELECT s FROM staion_info s WHERE LOWER(s.stationName) = LOWER(:stationName)")
    Optional<staion_info> findFirstByStationNameIgnoreCase(String stationName);

    // 정규화명(괄호/공백 제거된 키)이 따로 있을 경우에만 사용, 없다면 주석처리
    @Query("SELECT s FROM staion_info s WHERE s.stationName = :normalized")
    Optional<staion_info> findTop1ByStationNameNormalized(String normalized);

    // 라인 이름이 station_info에 없다면 이 메서드는 사용하지 않는다
    @Query("SELECT s FROM staion_info s WHERE s.stationName = :stationName AND :lineName IS NOT NULL")
    Optional<staion_info> findTop1ByStationNameAndLineName(String stationName, String lineName);
    @Query("SELECT s FROM staion_info s WHERE s.stationName = :stationName  AND ( s.lineName = :lineName OR s.lineName = CONCAT(:lineName, '호선') OR REPLACE(s.lineName, '호선', '') = :lineName) ")
    Optional<staion_info> findFirstByStationAndFlexibleLine(String stationName, String lineName);

}
