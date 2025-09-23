package com.example.pp.repository;


import com.example.pp.entity.staion_info;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubwayStationRepository extends JpaRepository<staion_info, String> {

    @Query(value = """

    SELECT
      s.*
    FROM station_info s
    WHERE ST_Distance_Sphere(POINT(s.longitude, s.latitude), POINT(:lon, :lat)) <= 100
    ORDER BY ST_Distance_Sphere(POINT(s.longitude, s.latitude), POINT(:lon, :lat)) ASC;
      """, nativeQuery = true)
        List<staion_info> findAllWithinRadiusOrderByDistanceAsc(
            @Param("lat") double lat,
            @Param("lon") double lon
    );


    @Query(value = "SELECT * FROM station_info WHERE station_id = :code", nativeQuery = true)
    Optional<staion_info> findOneByStationId(@Param("code") String stationCode);

    @Query(value = "SELECT * FROM station_info WHERE station_name = :name", nativeQuery = true)
    Optional<staion_info> findOneByStationName(@Param("name") String stationName);

    // 추가: 대소문자 무시 정확 일치
    Optional<staion_info> findFirstByStationNameIgnoreCase(String stationName);

    // 추가: 괄호 제거 후 일치(지원 DB에 따라 replace 사용 가능)
    @Query(value = """
        select * from station_info
        where replace(replace(station_name, '(', ''), ')', '') = :normalized
        limit 1
    """, nativeQuery = true)
    Optional<staion_info> findOneByStationNameNormalized(@Param("normalized") String normalized);

    // 추가: 동일 이름 다건 대비(필요 시 사용)
    List<staion_info> findByStationNameIn(Collection<String> names);

    // 추가: (라인+역명) 우선 단일 조회로 중복 예외 예방
    @Query(value = """
        select * from station_info
        where station_name = :name and line_number = :line
        limit 1
    """, nativeQuery = true)
    Optional<staion_info> findTop1ByStationNameAndLineName(@Param("name") String stationName,
                                                           @Param("line") String lineName);
    // 괄호 제거 후 Top1
    @Query(value = """
        select * from station_info
        where replace(replace(station_name, '(', ''), ')', '') = :normalized
        order by station_id
        limit 1
    """, nativeQuery = true)
    Optional<staion_info> findTop1ByStationNameNormalized(@Param("normalized") String normalized);


}
