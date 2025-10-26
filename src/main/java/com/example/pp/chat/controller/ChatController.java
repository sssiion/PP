package com.example.pp.chat.controller;


import com.example.pp.chat.dto.ServerMessageResponse;
import com.example.pp.chat.dto.UserMessageRequest;
import com.example.pp.chat.service.chatservice.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public Mono<ServerMessageResponse> chat(@RequestBody UserMessageRequest req, WebSession session) {
        if (req.getSessionId() == null || req.getSessionId().isBlank()) {
            // 세션 키를 응답에도 돌려주면 프런트가 다음 요청부터 보낼 수 있음
            req.setSessionId((String) session.getAttributes()
                    .computeIfAbsent("sid", k -> UUID.randomUUID().toString()));
        }
        return chatService.processChatRequest(req, session);
    }

    @GetMapping("/result/{sessionId}")
    public Mono<Object> getResultFromSession(@PathVariable String sessionId, WebSession session){
        Object data = session.getAttributes().get(sessionId);
        return Mono.justOrEmpty(data).switchIfEmpty(Mono.just("no data"));
    }
}
