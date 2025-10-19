package com.example.pp.rec.controller;

import com.example.pp.data.service.DetailService;
import com.example.pp.rec.dto.List1UserResponse;
import com.example.pp.rec.service.List2SeoulAreaService;
import com.example.pp.rec.service.List3BuildService;
import com.example.pp.rec.service.list1.list1_orchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final list1_orchestrator list1Service;     // 위치기반
    private final List2SeoulAreaService list2Service;   // 지역기반(contentid만)
    private final List3BuildService list3Service;       // 교집합(최종)
    private final DetailService detailService;

    @GetMapping("/")
    public  Mono<List<Map<String,Object>>> list3(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam(defaultValue = "5000") int radius,
            @RequestParam(defaultValue = "2000") int pageSize,
            @RequestParam(required = false) List<Integer> types
    ){
        List<String> categories = null;
        if (types != null && !types.isEmpty()) {
            categories = types.stream().map(type -> switch (type) {
                case 12 -> "tourist_attraction";
                case 14 -> "cultural_facilities";
                case 15 -> "festivals_performances_events";
                case 25 -> "travel_course";
                case 28 -> "leisure_sports";
                case 32 -> "accommodation";
                case 38 -> "shopping";
                case 39 -> "food";
                default -> null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }

        // List1: 위치기반 (TourAPI 원본 아이템)
        Mono<List<Map<String,Object>>> list1Mono =
                list1Service.build(lat, lon, time, radius, pageSize, categories);

        // List2: 지역기반 contentid (단일 페이지 전량)
        //Mono<java.util.List<String>> list2IdsMono =
               // list2Service.buildAndStore(10000);

        // List3: 교집합
        //return list3Service.buildAndStore(list1Mono, list2IdsMono, null);.
        return list1Mono;
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
        return list1Service.buildResult(lat, lon, time, radius, pageSize, String.valueOf(type)); //500
    }

    @GetMapping("/detail/{category}/{id}")
    public ResponseEntity<?> getDetail(@PathVariable String category, @PathVariable String id) {
        return detailService.getDetail(category, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
