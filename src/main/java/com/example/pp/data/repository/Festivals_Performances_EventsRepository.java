package com.example.pp.data.repository;

import com.example.pp.data.entity.Festivals_Performances_Events;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface Festivals_Performances_EventsRepository extends JpaRepository<Festivals_Performances_Events, String> {
    @Query(value = """
    SELECT a.id AS id,
           a.name AS name,
           CAST(a.latitude  AS double precision)  AS lat,
           CAST(a.longitude AS double precision)  AS lon,
           ST_Distance(
             geography(ST_SetSRID(ST_Point(CAST(a.longitude AS double precision), CAST(a.latitude AS double precision)), 4326)),
             geography(ST_SetSRID(ST_Point(:lon, :lat), 4326))
           ) AS distanceMeters
    FROM accommodation a
    WHERE ST_DWithin(
      geography(ST_SetSRID(ST_Point(CAST(a.longitude AS double precision), CAST(a.latitude AS double precision)), 4326)),
      geography(ST_SetSRID(ST_Point(:lon, :lat), 4326)),
      :radiusMeters
    )
    ORDER BY distanceMeters ASC
    """, nativeQuery = true)
    List<NearbyRow> findWithinMeters(@Param("lat") double lat,
                                     @Param("lon") double lon,
                                     @Param("radiusMeters") double radiusMeters);

    @Query(value = """
    SELECT a.*
    FROM Festivals_Performances_Events a
    WHERE ST_DWithin(
      geography(ST_SetSRID(ST_Point(:lon, :lat), 4326)),
      geography(ST_SetSRID(ST_Point(CAST(a.longitude AS double precision),
                                    CAST(a.latitude  AS double precision)), 4326)),
      :radiusMeters
    )
  """, nativeQuery = true)
    List<Festivals_Performances_Events> findEntitiesWithinRadius(@Param("lat") double lat,
                                                                @Param("lon") double lon,
                                                                @Param("radiusMeters") double radiusMeters);

}
