package com.example.pp.chat.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
@Configuration
@RequiredArgsConstructor
public class HttpClientConfig {

   

    @Bean
    WebClient naverWebClient(WebClient.Builder b,
                             @Value("${naver.client-id:}") String id,
                             @Value("${naver.client-secret:}") String secret) {
        String cid = id.isBlank()? System.getenv("NAVER_CLIENT_ID") : id;
        String csec = secret.isBlank()? System.getenv("NAVER_CLIENT_SECRET") : secret;
        return b.baseUrl("https://openapi.naver.com")
                .defaultHeader("X-Naver-Client-Id", cid)
                .defaultHeader("X-Naver-Client-Secret", csec)
                .build();
    }

    @Bean
    WebClient geminiWebClient(WebClient.Builder b) {
        return b.baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type","application/json")
                .defaultHeader("x-goog-api-key", "AIzaSyCwqFuqZ2cuV9gaAAKq2_fmgCB-UyjyqkY")
                .build();
    }

    @Bean
    public RateLimiter kakaoLimiter() {
        return RateLimiter.of("kakaoLimiter", RateLimiterConfig.custom()
                .limitForPeriod(8)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ZERO)
                .build());
    }

    @Bean
    public RateLimiter naverLimiter() {
        return RateLimiter.of("naverLimiter", RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ZERO)
                .build());
    }

    @Bean
    public Retry externalRetry() {
        return Retry.of("externalRetry", RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(300))
                .build());
    }
}
