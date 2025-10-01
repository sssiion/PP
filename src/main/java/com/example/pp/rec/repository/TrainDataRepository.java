package com.example.pp.rec.repository;


import com.example.pp.rec.entity.train_data;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TrainDataRepository extends JpaRepository<train_data, Long> {

    // 지정 시간창 내 도착 열차 존재 여부
    boolean existsByLineCodeAndStationNameAndDayTypeAndArrTimeBetween(
            String lineCode, String stationName, String dayType, LocalTime start, LocalTime end
    );

    List<train_data> findByStationNameAndDayTypeAndArrTimeBetweenOrderByArrTimeAsc(
            String stationName, String dayType, LocalTime start, LocalTime end
    );
    // 지정 시각 이후 가장 빠른 열차 1건(방향 판단용)
    Optional<train_data> findTop1ByLineCodeAndStationNameAndDayTypeAndArrTimeGreaterThanEqualOrderByArrTimeAsc(
            String lineCode, String stationName, String dayType, LocalTime time
    );

}
