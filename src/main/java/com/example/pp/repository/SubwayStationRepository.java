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
