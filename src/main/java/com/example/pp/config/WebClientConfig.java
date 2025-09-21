package com.example.pp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    private static ExchangeFilterFunction errorFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> {
            if (resp.statusCode().isError()) return resp.createException().flatMap(Mono::error);
            return Mono.just(resp);
        });
    }

    // Tour: 단일 게이트웨이
    @Bean(name = "tourWebClient")
    public WebClient tourWebClient(WebClient.Builder builder,
                                   @Value("${tour.api.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).filter(errorFilter()).build();
    }

    // TAGO: 데이터셋별 게이트웨이
    @Bean(name = "tagoBusStopWebClient")
    public WebClient tagoBusStopWebClient(WebClient.Builder builder,
                                          @Value("${tago.busstop.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).filter(errorFilter()).build();
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
}
