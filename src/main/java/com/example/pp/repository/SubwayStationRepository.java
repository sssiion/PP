package com.example.pp.repository;


import com.example.pp.entity.SubwayStation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubwayStationRepository extends JpaRepository<SubwayStation, String> {

    @Query(value = """
        SELECT
          s.*
        FROM subway_station s
        WHERE
          (6371000 * 2 * ASIN(
             SQRT(
               POW(SIN(RADIANS((:lat - s.lat) / 2)), 2) +
               COS(RADIANS(:lat)) * COS(RADIANS(s.lat)) *
               POW(SIN(RADIANS((:lon - s.lon) / 2)), 2)
             )
          )) <= :radiusMeters
        ORDER BY
          (6371000 * 2 * ASIN(
             SQRT(
               POW(SIN(RADIANS((:lat - s.lat) / 2)), 2) +
               COS(RADIANS(:lat)) * COS(RADIANS(s.lat)) *
               POW(SIN(RADIANS((:lon - s.lon) / 2)), 2)
             )
          )) ASC
        """,
            nativeQuery = true)
    List<SubwayStation> findAllWithinRadiusOrderByDistanceAsc(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters
    );

    @Query(value = """
        SELECT
          s.station_id    AS stationId,
          s.station_name  AS stationName,
          s.line_no       AS lineNo,
          s.line_name     AS lineName,
          s.lat           AS lat,
          s.lon           AS lon,
          s.road_address  AS roadAddress,
          (6371000 * 2 * ASIN(
             SQRT(
               POW(SIN(RADIANS((:lat - s.lat) / 2)), 2) +
               COS(RADIANS(:lat)) * COS(RADIANS(s.lat)) *
               POW(SIN(RADIANS((:lon - s.lon) / 2)), 2)
             )
          ))              AS distance
        FROM subway_station s
        WHERE
          (6371000 * 2 * ASIN(
             SQRT(
               POW(SIN(RADIANS((:lat - s.lat) / 2)), 2) +
               COS(RADIANS(:lat)) * COS(RADIANS(s.lat)) *
               POW(SIN(RADIANS((:lon - s.lon) / 2)), 2)
             )
          )) <= :radiusMeters
        ORDER BY distance ASC
        """,
            nativeQuery = true)
    List<SubwayStationDistanceView> findAllWithinRadiusWithDistanceOrderByDistanceAsc(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters
    );
}
