package com.example.pp.rec.service;

import com.example.pp.rec.dto.CongestionRequestDto;
import com.example.pp.rec.dto.CongestionResponseDto;
import com.example.pp.rec.dto.LocationDto; // New import
import com.example.pp.rec.dto.PythonCongestionRequest; // New import
import com.example.pp.rec.dto.PythonCongestionResponse; // New import
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream; // New import

@Service
public class CongestionService {

    private final WebClient webClient;

    public CongestionService(@Qualifier("congestionWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<List<CongestionResponseDto>> getCongestion(List<CongestionRequestDto> requestDtos) {
        if (requestDtos == null || requestDtos.isEmpty()) {
            return Mono.just(List.of());
        }

        // Assuming datetime is consistent across all requests in the batch for the Python API
        String commonDatetime = requestDtos.get(0).getDatetime();

        List<LocationDto> locations = requestDtos.stream()
                .map(dto -> new LocationDto(dto.getLatitude(), dto.getLongitude()))
                .collect(Collectors.toList());

        PythonCongestionRequest pythonRequest = new PythonCongestionRequest(commonDatetime, locations);

        return webClient.post()
                .uri("/get-congestion") // Python API endpoint
                .bodyValue(pythonRequest)
                .retrieve()
                .bodyToMono(PythonCongestionResponse.class) // Expect PythonCongestionResponse
                .map(pythonResponse -> {
                    List<String> congestionLevels = pythonResponse.getCongestion_levels();
                    // Map Python's response back to our CongestionResponseDto list
                    // Assuming the order of congestion_levels matches the order of locations in the request
                    return IntStream.range(0, requestDtos.size())
                            .mapToObj(index -> {
                                CongestionRequestDto reqDto = requestDtos.get(index);
                                String congestionLevel = congestionLevels.size() > index ? congestionLevels.get(index) : "Unknown";
                                return new CongestionResponseDto(
                                        reqDto.getLatitude(),
                                        reqDto.getLongitude(),
                                        reqDto.getDatetime(),
                                        congestionLevel
                                );
                            })
                            .collect(Collectors.toList());
                })
                .onErrorResume(WebClientRequestException.class, e -> {
                    if (e.getCause() instanceof ConnectException) {
                        // Handle connection refused specifically
                        // Return a list of error responses for each requested item
                        List<CongestionResponseDto> errorResponses = requestDtos.stream()
                                .map(req -> new CongestionResponseDto(
                                        req.getLatitude(),
                                        req.getLongitude(),
                                        req.getDatetime(),
                                        ""
                                ))
                                .collect(Collectors.toList());
                        return Mono.just(errorResponses);
                    }
                    // For other WebClientRequestExceptions, rethrow or handle differently
                    return Mono.error(e);
                });
    }
}
