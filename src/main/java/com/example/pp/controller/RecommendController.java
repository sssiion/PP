package com.example.pp.controller;

import com.example.pp.dto.List1UserResponse;
import com.example.pp.service.List2SeoulAreaService;
import com.example.pp.service.List3BuildService;
import com.example.pp.service.list1.list1_orchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalTime;
import java.util.List;


@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final list1_orchestrator list1Service;     // 위치기반
    private final List2SeoulAreaService list2Service;   // 지역기반(contentid만)
    private final List3BuildService list3Service;       // 교집합(최종)

    // 예: /api/recommend/list3?lat=37.5557&lon=126.9730&time=13:30:00&radius=8000&pageSize=200
    @GetMapping("/")
    public Mono<List<List3BuildService.List3Item>> list3(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam(defaultValue = "5000") int radius,
            @RequestParam(defaultValue = "2000") int pageSize,
            @RequestParam(defaultValue= "12") int type
    ){
        // List1: 위치기반 (TourAPI 원본 아이템)
        Mono<java.util.List<com.example.pp.dto.TourPoiResponse.Item>> list1Mono =
                list1Service.build(lat, lon, time, radius, pageSize, type);

        // List2: 지역기반 contentid (단일 페이지 전량)
        Mono<java.util.List<String>> list2IdsMono =
                list2Service.buildAndStore(10000);

        // List3: 교집합
        return list3Service.buildAndStore(list1Mono, list2IdsMono, null);
    }
    @GetMapping("/list1-detail")
    public Mono<List1UserResponse> list1Detail(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam(defaultValue = "5000") int radius,
            @RequestParam(defaultValue = "2000") int pageSize,
            @RequestParam(defaultValue= "12") int type
    ){
        return list1Service.buildResult(lat, lon, time, radius, pageSize, type);
    }
}
