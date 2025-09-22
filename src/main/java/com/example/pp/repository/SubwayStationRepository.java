package com.example.pp.repository;


import com.example.pp.entity.staion_info;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubwayStationRepository extends JpaRepository<staion_info, String> {

    @Query(value = """
      SELECT
        s.*
      FROM station_info s
      WHERE
        (6371000 * 2 * ASIN(
           SQRT(
             POW(SIN(RADIANS((:lat - s.latitude) / 2)), 2) +
             COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) *
             POW(SIN(RADIANS((:lon - s.longitude) / 2)), 2)
           )
        )) <= :radiusMeters
      ORDER BY
        (6371000 * 2 * ASIN(
           SQRT(
             POW(SIN(RADIANS((:lat - s.latitude) / 2)), 2) +
             COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) *
             POW(SIN(RADIANS((:lon - s.longitude) / 2)), 2)
           )
        )) ASC
      """, nativeQuery = true)
        List<staion_info> findAllWithinRadiusOrderByDistanceAsc(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters
    );

    @Query(value = """
  SELECT
    s.station_id    AS stationId,
    s.station_name  AS stationName,
    ''              AS lineNo,      -- 테이블에 없으면 임시 별칭 또는 인터페이스에서 제거
    s.line_number   AS lineName,
    s.latitude      AS lat,
    s.longitude     AS lon
    (6371000 * 2 * ASIN(
       SQRT(
         POW(SIN(RADIANS((:lat - s.latitude) / 2)), 2) +
         COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) *
         POW(SIN(RADIANS((:lon - s.longitude) / 2)), 2)
       )
    ))              AS distance
  FROM station_info s
  WHERE
    (6371000 * 2 * ASIN(
       SQRT(
         POW(SIN(RADIANS((:lat - s.latitude) / 2)), 2) +
         COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) *
         POW(SIN(RADIANS((:lon - s.longitude) / 2)), 2)
       )
    )) <= :radiusMeters
  ORDER BY distance ASC
  """, nativeQuery = true)
    List<SubwayStationDistanceView> findAllWithinRadiusWithDistanceOrderByDistanceAsc(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters
    );
    @Query(value = "SELECT * FROM station_info WHERE station_id = :code", nativeQuery = true)
    Optional<staion_info> findOneByStationId(@Param("code") String stationCode);

    @Query(value = "SELECT * FROM station_info WHERE station_name = :name", nativeQuery = true)
    Optional<staion_info> findOneByStationName(@Param("name") String stationName);
}
