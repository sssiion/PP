package com.example.pp.rec.service;

import com.example.pp.rec.dto.CongestionRequestDto;
import com.example.pp.rec.dto.CongestionResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CongestionService {

    private final WebClient webClient;

    public CongestionService(@Qualifier("congestionWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<List<CongestionResponseDto>> getCongestion(List<CongestionRequestDto> requestDtos) {
        return webClient.post()
                .uri("/get-congestion")
                .bodyValue(requestDtos)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CongestionResponseDto>>() {})
                .onErrorResume(WebClientRequestException.class, e -> {
                    if (e.getCause() instanceof ConnectException) {
                        // Handle connection refused specifically
                        // Return a list of error responses for each requested item
                        List<CongestionResponseDto> errorResponses = requestDtos.stream()
                                .map(req -> new CongestionResponseDto(
                                        Double.parseDouble(req.getLat()),
                                        Double.parseDouble(req.getLon()),
                                        req.getDatetime(),
                                        "Error: Python server connection refused"
                                ))
                                .collect(Collectors.toList());
                        return Mono.just(errorResponses);
                    }
                    // For other WebClientRequestExceptions, rethrow or handle differently
                    return Mono.error(e);
                });
    }
}
