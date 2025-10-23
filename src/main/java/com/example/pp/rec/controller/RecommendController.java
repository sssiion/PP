package com.example.pp.rec.controller;

import com.example.pp.data.service.DetailService;
import com.example.pp.rec.dto.List1UserResponse;
import com.example.pp.rec.service.CombinedRecommendationService;
import com.example.pp.rec.service.List2SeoulAreaService;
import com.example.pp.rec.service.List3BuildService;
import com.example.pp.rec.service.list1.list1_orchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalTime;
import java.time.LocalDateTime;
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
    private final CombinedRecommendationService combinedService; // 신규 서비스 주입

    // DTO for the main recommendation request
    public record RecommendRequest(Double lat, Double lon, LocalTime time, Integer radius, Integer pageSize, List<Integer> types, LocalDateTime congestionDateTime) {}

    private Mono<List<Map<String, Object>>> handleRecommendation(RecommendRequest request) {
        List<String> categories = null;
        if (request.types() != null && !request.types().isEmpty()) {
            categories = request.types().stream().map(type -> switch (type) {
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

        return list1Service.build(request.lat(), request.lon(), request.time(), request.radius(), request.pageSize(), categories);
    }

    private Mono<List<Map<String, Object>>> handleRecommendationWithCongestion(RecommendRequest request) {
        List<String> categories = null;
        if (request.types() != null && !request.types().isEmpty()) {
            categories = request.types().stream().map(type -> switch (type) {
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

        return combinedService.recommendWithCongestion(request.lat(), request.lon(), request.time(), request.radius(), request.pageSize(), categories, request.congestionDateTime());
    }

    @GetMapping("/")
    public Mono<List<Map<String,Object>>> list3Get(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam(defaultValue = "5000") int radius,
            @RequestParam(defaultValue = "2000") int pageSize,
            @RequestParam(required = false) List<Integer> types
    ){
        RecommendRequest request = new RecommendRequest(lat, lon, time, radius, pageSize, types, null);
        return handleRecommendation(request);
    }

    @PostMapping("/")
    public Mono<List<Map<String, Object>>> list3Post(@RequestBody RecommendRequest request) {
        return handleRecommendation(request);
    }

    @GetMapping("/with-congestion")
    public Mono<List<Map<String,Object>>> list3GetWithCongestion(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime congestionDateTime, // 혼잡도 시간 파라미터 추가
            @RequestParam(defaultValue = "5000") int radius,
            @RequestParam(defaultValue = "2000") int pageSize,
            @RequestParam(required = false) List<Integer> types
    ){
        RecommendRequest request = new RecommendRequest(lat, lon, time, radius, pageSize, types, congestionDateTime);
        return handleRecommendationWithCongestion(request);
    }

    @PostMapping("/with-congestion")
    public Mono<List<Map<String, Object>>> list3PostWithCongestion(@RequestBody RecommendRequest request) {
        return handleRecommendationWithCongestion(request);
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

    @RequestMapping(value = "/detail/{category}/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> getDetail(@PathVariable String category, @PathVariable String id) {
        return detailService.getDetail(category, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Inner record for the POST request body
    public record DetailColumnRequest(List<String> ids, String column) {}

    @PostMapping("/detail/{category}/column")
    public ResponseEntity<Map<String, Object>> getDetailColumn(
            @PathVariable String category,
            @RequestBody DetailColumnRequest request) {
        Map<String, Object> result = detailService.getDetailsForColumn(category, request.ids(), request.column());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/detail/{category}/column")
    public ResponseEntity<Map<String, Object>> getDetailColumnGet(
            @PathVariable String category,
            @RequestParam List<String> ids,
            @RequestParam String column) {
        Map<String, Object> result = detailService.getDetailsForColumn(category, ids, column);
        return ResponseEntity.ok(result);
    }
}
