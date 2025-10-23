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
}
