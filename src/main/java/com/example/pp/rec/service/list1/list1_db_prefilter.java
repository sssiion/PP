package com.example.pp.rec.service.list1;


import com.example.pp.rec.entity.train_data;
import com.example.pp.rec.repository.TrainDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class list1_db_prefilter {
    // DB 선 필터: 역명+시간창으로 레코드 조회 후 lineCode별 가장 이른 도착만 남김
    private static final Logger log = LoggerFactory.getLogger(list1_db_prefilter.class);
    private final TrainDataRepository trainRepo;

    public list1_db_prefilter(TrainDataRepository trainRepo) { this.trainRepo = trainRepo; }

    // // 입력: 역명(접미사 보정 완료), dayType=DAY, 시작/종료 시각
    // // 처리: train_data에서 조건에 맞는 레코드 조회 → lineCode별 earliest 도착시간만 유지
    // // 출력: Map<lineCode, earliestArrTime>
    public Mono<Map<String, LocalTime>> earliestByLineCode(String stationNameEnsured, String dayType,
                                                           LocalTime start, LocalTime end) {
        return Mono.fromCallable(() ->
                        trainRepo.findByStationNameAndDayTypeAndArrTimeBetweenOrderByArrTimeAsc(
                                stationNameEnsured, dayType, start, end
                        )
                )
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map((List<train_data> list) -> {
                    Map<String, LocalTime> earliest = new LinkedHashMap<>();
                    for (train_data td : list) {
                        earliest.putIfAbsent(td.getLineCode(), td.getArrTime());
                    }
                    log.info("[List1] DB earliest map station='{}' size={}", stationNameEnsured, earliest.size());
                    return earliest;
                });
    }
    // list1_db_prefilter.java (발췌) — 라인 문자열 포함 수집
    public Mono<Map<String, LocalTime>> earliestByTrainLine(String stationNameEnsured, String dayType, LocalTime start, LocalTime end) {
        return Mono.fromCallable(() ->
                        trainRepo.findByStationNameAndDayTypeAndArrTimeBetweenOrderByArrTimeAsc(stationNameEnsured, dayType, start, end)
                )
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(list -> {
                    Map<String, LocalTime> earliest = new LinkedHashMap<>();
                    for (train_data td : list) {
                        String lineName = td.getLine(); // traindata에 라인 문자열 필드 추가(예: "1호선")
                        if (lineName == null) continue;
                        earliest.putIfAbsent(lineName, td.getArrTime());
                    }
                    return earliest;
                });
    }
}
