package com.example.pp.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))
                .authorizeExchange(authorize -> authorize
                        .pathMatchers("/", "/css/**", "/images/**", "/js/**", "/h2-console/**", "/login/**", "/oauth2/**", "/api/recommend/**", "/api/congestion/**").permitAll()
                        .anyExchange().authenticated()
                )
                .logout(logout -> logout.logoutUrl("/")) // Simplified logout
                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler((webFilterExchange, authentication) -> {
                            var response = webFilterExchange.getExchange().getResponse();
                            response.setStatusCode(HttpStatus.OK);
                            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                            byte[] body = "{\"status\":\"success\", \"message\":\"Login successful\"}".getBytes(StandardCharsets.UTF_8);
                            DataBuffer buffer = response.bufferFactory().wrap(body);
                            return response.writeWith(Mono.just(buffer));
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*", "*"));
        configuration.setAllowedMethods(Arrays.asList("*", "*"));
        configuration.setAllowedHeaders(Arrays.asList("*", "*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}