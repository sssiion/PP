package com.example.pp.rec.service;

import com.example.pp.rec.dto.CongestionRequestDto;
import com.example.pp.rec.dto.CongestionResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CongestionServiceTest {

    private MockWebServer mockWebServer;
    private CongestionService congestionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        congestionService = new CongestionService(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getCongestion_should_return_congestion_list_for_valid_locations() throws JsonProcessingException, InterruptedException {
        // given: 테스트 준비
        List<CongestionRequestDto> requestDtos = List.of(
                new CongestionRequestDto("37.5665", "126.9780", "2025-10-22T14:30:00"),
                new CongestionRequestDto("37.5796", "126.9770", "2025-10-22T15:00:00")
        );

        List<CongestionResponseDto> expectedResponse = List.of(
                new CongestionResponseDto(37.5665, 126.9780, "2025-10-22T14:30:00", "붐빔"),
                new CongestionResponseDto(37.5796, 126.9770, "2025-10-22T15:00:00", "보통")
        );

        // 가짜 서버가 반환할 응답을 설정
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        // when: 테스트할 메소드 호출
        Mono<List<CongestionResponseDto>> resultMono = congestionService.getCongestion(requestDtos);

        // then: 결과 검증
        StepVerifier.create(resultMono)
                .expectNextMatches(responseList -> {
                    assertThat(responseList).hasSize(2);
                    assertThat(responseList.get(0).getCongestionLevel()).isEqualTo("붐빔");
                    assertThat(responseList.get(1).getCongestionLevel()).isEqualTo("보통");
                    return true;
                })
                .verifyComplete();

        // 가짜 서버가 받은 요청을 검증
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/get-congestion");
        assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(objectMapper.writeValueAsString(requestDtos));
    }
}
