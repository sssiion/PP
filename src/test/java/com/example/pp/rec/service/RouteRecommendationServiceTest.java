package com.example.pp.rec.service;

import com.example.pp.rec.dto.CongestionResponseDto;
import com.example.pp.rec.dto.RouteResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteRecommendationServiceTest {

    @Mock
    private TmapService tmapService;

    @Mock
    private CongestionService congestionService;

    @InjectMocks
    private RouteRecommendationService routeRecommendationService;

    private String mockTmapResponse;

    @BeforeEach
    void setUp() {
        // 테스트에 사용할 Tmap API의 가짜 응답 데이터
        // 경로 1: 시간 1200초, 거리 2000미터
        // 경로 2: 시간 1500초, 거리 2200미터
        mockTmapResponse = """
        {
            "type": "FeatureCollection",
            "features": [
                {
                    "type": "Feature",
                    "geometry": {
                        "type": "LineString",
                        "coordinates": [[126.9780, 37.5665], [126.9785, 37.5670]]
                    },
                    "properties": { "totalTime": 1200, "totalDistance": 2000 }
                },
                {
                    "type": "Feature",
                    "geometry": {
                        "type": "LineString",
                        "coordinates": [[126.9780, 37.5665], [126.9790, 37.5675]]
                    },
                    "properties": { "totalTime": 1500, "totalDistance": 2200 }
                }
            ]
        }
        """;
    }

    @Test
    @DisplayName("혼잡도순 정렬 시, 혼잡도 점수가 낮고 시간이 빠른 순으로 정렬되어야 한다")
    void should_sortRoutesByCongestion_whenSortParamIsCongestion() {
        // given: 테스트 준비
        // 경로 1은 더 빠르지만, 혼잡함
        List<CongestionResponseDto> congestionForRoute1 = List.of(
            new CongestionResponseDto(37.5665, 126.9780, "", "붐빔"), // 8점
            new CongestionResponseDto(37.5670, 126.9785, "", "붐빔")  // 8점
        ); // 평균 8점

        // 경로 2는 더 느리지만, 여유로움
        List<CongestionResponseDto> congestionForRoute2 = List.of(
            new CongestionResponseDto(37.5665, 126.9780, "", "여유"), // 1점
            new CongestionResponseDto(37.5675, 126.9790, "", "보통")  // 3점
        ); // 평균 2점

        // Mock 설정: 서비스가 특정 입력을 받았을 때 어떤 출력을 반환할지 정의
        when(tmapService.getRoute(any(Double.class), any(Double.class), any(Double.class), any(Double.class), any(String.class)))
            .thenReturn(Mono.just(mockTmapResponse));
        // any()는 어떤 값이 들어오든 상관없이 반응하라는 의미
        when(congestionService.getCongestion(any()))
            .thenReturn(Mono.just(congestionForRoute1)) // 첫번째 호출 시
            .thenReturn(Mono.just(congestionForRoute2)); // 두번째 호출 시

        // when: 테스트할 메소드 호출
        Mono<RouteResponseDto> result = routeRecommendationService.getRouteWithCongestion(1.0, 1.0, 2.0, 2.0, "congestion", null, (String) null);

        // then: 결과 검증
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                List<RouteResponseDto.RecommendedRoute> routes = response.getRoutes();
                assertThat(routes).hasSize(2);
                // 경로 2 (혼잡도 2점)가 경로 1 (혼잡도 8점)보다 먼저 와야 함
                assertThat(routes.get(0).getCongestionScore()).isLessThan(routes.get(1).getCongestionScore());
                assertThat(routes.get(0).getDurationInSeconds()).isEqualTo(1500); // 경로 2의 시간
                assertThat(routes.get(1).getDurationInSeconds()).isEqualTo(1200); // 경로 1의 시간
                return true;
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("시간순 정렬 시, 소요 시간이 짧은 순으로 정렬되어야 한다")
    void should_sortRoutesByDuration_whenSortParamIsDuration() {
        // given: 테스트 준비 (혼잡도 설정은 위와 동일)
        List<CongestionResponseDto> congestionForRoute1 = List.of(new CongestionResponseDto(0,0,"","붐빔"));
        List<CongestionResponseDto> congestionForRoute2 = List.of(new CongestionResponseDto(0,0,"","여유"));

        when(tmapService.getRoute(any(Double.class), any(Double.class), any(Double.class), any(Double.class), any(String.class)))
            .thenReturn(Mono.just(mockTmapResponse));
        when(congestionService.getCongestion(any()))
            .thenReturn(Mono.just(congestionForRoute1))
            .thenReturn(Mono.just(congestionForRoute2));

        // when: 테스트할 메소드 호출
        Mono<RouteResponseDto> result = routeRecommendationService.getRouteWithCongestion(1.0, 1.0, 2.0, 2.0, "duration", null, (String) null);

        // then: 결과 검증
        StepVerifier.create(result)
            .expectNextMatches(response -> {
                List<RouteResponseDto.RecommendedRoute> routes = response.getRoutes();
                assertThat(routes).hasSize(2);
                // 경로 1 (1200초)이 경로 2 (1500초)보다 먼저 와야 함
                assertThat(routes.get(0).getDurationInSeconds()).isLessThan(routes.get(1).getDurationInSeconds());
                assertThat(routes.get(0).getDurationInSeconds()).isEqualTo(1200);
                assertThat(routes.get(1).getDurationInSeconds()).isEqualTo(1500);
                return true;
            })
            .verifyComplete();
    }
}
