package com.example.pp.chat.controller;


import com.example.pp.chat.dto.RecommendedPlace;
import com.example.pp.chat.dto.ServerMessageResponse;
import com.example.pp.chat.dto.UserMessageRequest;
import com.example.pp.chat.repository.PlaceStore;
import com.example.pp.chat.service.chatservice.ChatFlowService;
import com.example.pp.chat.service.chatservice.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;// com/example/pp/chat/controller/ChatController.java
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatFlowService flow;

    @PostMapping
    public Mono<ServerMessageResponse> chat(@RequestBody UserMessageRequest req, WebSession session) {
        return flow.runPipeline(req, session)
                .onErrorResume(ex -> {
                    if ("NEED_LOCATION".equals(ex.getMessage())) {
                        return Mono.just(ServerMessageResponse.text("좌표가 없어요. [translate:홍대입구역], [translate:강남역] 같은 기준 위치를 말하거나 현재 위치를 공유해 주세요."));
                    }
                    return Mono.just(ServerMessageResponse.error("일시적 오류가 발생했어요. 잠시 후 다시 시도해주세요."));
                });
    }
}
