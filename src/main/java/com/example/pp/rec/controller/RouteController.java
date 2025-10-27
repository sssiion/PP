package com.example.pp.rec.controller;

import com.example.pp.rec.dto.RouteRequestDto;
import com.example.pp.rec.dto.RouteResponseDto;
import com.example.pp.rec.service.RouteRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class RouteController {

    private final RouteRecommendationService routeRecommendationService;

    @PostMapping
    public Mono<RouteResponseDto> getRecommendedRoute(@RequestBody RouteRequestDto requestDto) {
        return routeRecommendationService.getRouteWithCongestion(
            requestDto.getStartX(),
            requestDto.getStartY(),
            requestDto.getEndX(),
            requestDto.getEndY(),
            requestDto.getSort(),
            requestDto.getMode(),
            requestDto.getDepartureTime()
        );
    }
}
