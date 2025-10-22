package com.example.pp.rec.config;

import io.netty.handler.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Configuration
public class WebClientConfig {
    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);
    private static ExchangeFilterFunction errorFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> {
            if (resp.statusCode().isError()) return resp.createException().flatMap(Mono::error);
            return Mono.just(resp);
        });
    }

    @Bean(name = "tourWebClient")
    public WebClient tourWebClient(WebClient.Builder builder,
                                   @Value("${tour.api.base-url}") String baseUrl) {
        DefaultUriBuilderFactory f = new DefaultUriBuilderFactory(baseUrl);
        // 값 중심 인코딩(템플릿은 그대로, 변수 값만 인코딩)
        f.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY); // 대안: URI_COMPONENT
        return builder.baseUrl(baseUrl)
                .uriBuilderFactory(f)
                .build();
    }

    @Bean(name = "tagoBusStopWebClient")
    public WebClient tagoBusStopWebClient(WebClient.Builder builder,
                                          @Value("${tago.busstop.base-url}") String baseUrl) {
        return base(builder, baseUrl);
    }

    @Bean(name = "tagoArrivalWebClient")
    public WebClient tagoArrivalWebClient(WebClient.Builder builder,
                                          @Value("${tago.arrival.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).filter(errorFilter()).build();
    }

    @Bean(name = "tagoRouteWebClient")
    public WebClient tagoRouteWebClient(WebClient.Builder builder,
                                        @Value("${tago.route.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).filter(errorFilter()).build();
    }

    @Bean(name = "tagoBusLocWebClient")
    public WebClient tagoBusLocWebClient(WebClient.Builder builder,
                                         @Value("${tago.busloc.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).filter(errorFilter()).build();
    }

    @Bean(name = "tagoSubwayWebClient")
    public WebClient tagoSubwayWebClient(WebClient.Builder builder,
                                         @Value("${tago.subway.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).filter(errorFilter()).build();
    }

    @Bean(name = "tagoTrainWebClient")
    public WebClient tagoTrainWebClient(WebClient.Builder builder,
                                        @Value("${tago.train.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).filter(errorFilter()).build();
    }

    // Seoul Metro OpenAPI (http://openapi.seoul.go.kr:8088)
    @Bean(name = "seoulMetroWebClient")
    public WebClient seoulMetroWebClient(WebClient.Builder builder,
                                         @Value("${seoulmetro.api.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).filter(errorFilter()).build();
    }

    @Bean(name = "congestionWebClient")
    public WebClient congestionWebClient(WebClient.Builder builder,
                                         @Value("${congestion.api.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl)
                .filter(logRequestKo())
                .filter(logResponseKo())
                .filter(errorFilter())
                .build();
    }

    private static ExchangeFilterFunction logRequestKo() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            log.info("[외부호출 시작] 메서드={}, URL={}, 헤더={}", req.method(), req.url(), req.headers()); // 한국어 요약 [web:77]
            return Mono.just(req);
        });
    }

    private static ExchangeFilterFunction logResponseKo() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> {
            log.info("[외부호출 응답] 상태={}, 헤더={}", resp.statusCode(), resp.headers().asHttpHeaders()); // 한국어 요약 [web:77]
            return Mono.just(resp);
        });
    }

    private ReactorClientHttpConnector wiretapConnector() {
        HttpClient http = HttpClient.create()
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL); // 상세 네트워크 로그 [web:85]
        return new ReactorClientHttpConnector(http);
    }

    private WebClient base(WebClient.Builder builder, String baseUrl) {
        return builder
                .clientConnector(wiretapConnector())     // 네트워크 로그 [web:85]
                .baseUrl(baseUrl)
                .filter(logRequestKo())                  // 한국어 요청 요약 [web:77]
                .filter(logResponseKo())                 // 한국어 응답 요약 [web:77]
                .filter(errorFilter())                   // 기존 에러 전파 필터 [web:77]
                .build();
    }

}
