package com.example.pp.chat.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
public class ChatSseController {

    @GetMapping(path = "/api/chat/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> progress() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(tick -> switch ((int)(tick % 4)) {
                    case 0 -> "의도 파악 중...";
                    case 1 -> "위치/혼잡도 확인 중...";
                    case 2 -> "장소 검색/강화 중...";
                    default -> "최종 선별/정리 중...";
                })
                .take(8);
    }
}
