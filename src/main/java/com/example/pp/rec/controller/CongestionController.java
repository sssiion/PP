package com.example.pp.rec.controller;

import com.example.pp.rec.dto.CongestionRequestDto;
import com.example.pp.rec.dto.CongestionResponseDto;
import com.example.pp.rec.service.CongestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/congestion")
@RequiredArgsConstructor
public class CongestionController {

    private final CongestionService congestionService;

    // POST endpoint for batch processing (efficient for multiple locations)
    @PostMapping
    public Mono<List<CongestionResponseDto>> getCongestionBatch(@RequestBody List<CongestionRequestDto> requestDtos) {
        return congestionService.getCongestion(requestDtos);
    }

    // GET endpoint for single location lookup (convenient for browser testing)
    @GetMapping
    public Mono<CongestionResponseDto> getCongestionSingle(
            @RequestParam("lat") String lat,
            @RequestParam("lon") String lon,
            @RequestParam("datetime") String datetime) {

        // Create a request for a single location
        CongestionRequestDto singleRequest = new CongestionRequestDto(lat, lon, datetime);

        // Wrap it in a list to use the existing batch service
        List<CongestionRequestDto> requestList = List.of(singleRequest);

        // Call the service and extract the single result from the response list
        return congestionService.getCongestion(requestList)
                .map(responseList -> responseList.isEmpty() ? null : responseList.get(0));
    }
}
