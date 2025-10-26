package com.example.pp.chat.config;



import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class MetricsConfig {

    @Bean
    public Timer chatTimer(MeterRegistry registry){
        return Timer.builder("chat.process.timer").register(registry);
    }
}