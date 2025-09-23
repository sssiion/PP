package com.example.pp.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig  implements WebMvcConfigurer{
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // /api/ 로 시작하는 모든 요청에 대해
                .allowedOrigins("https://ganglngreact-production-fb31.up.railway.app") // http://localhost:3000 (React 개발서버)의 요청을 허용
                .allowedOrigins("https://localhost:8080") //https://ppfront-production.up.railway.app/
                .allowedOrigins("https://ppfront-production.up.railway.app")
                .allowedMethods("GET", "POST", "PUT", "DELETE") // 허용할 HTTP 메서드
                .allowCredentials(true);
    }
}
