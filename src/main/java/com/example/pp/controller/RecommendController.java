package com.example.pp.controller;

import com.example.pp.service.List3BuildService;
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

    private final List3BuildService service;

    // 예: /api/recommend/list3?lat=37.5557&lon=126.9730&time=13:30:00&windowMin=15&radius=8000&pageSize=200
    @GetMapping("/list3")
    public Mono<List<List3BuildService.List3Item>> list3(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam(defaultValue = "15") int windowMin,
            @RequestParam(defaultValue = "8000") int radius,
            @RequestParam(defaultValue = "200") int pageSize
    ){
        return service.build(lat, lon, time, windowMin, radius, pageSize);
    }
}
