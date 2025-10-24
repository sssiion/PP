package com.example.pp.rec.service;

import com.example.pp.rec.dto.ServerMessageResponse;
import com.example.pp.rec.dto.UserMessageRequest;
import com.example.pp.rec.entity.ChatContext;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;


public interface ChatOrchestrationService {
    Mono<ServerMessageResponse> processMessage(ChatContext context, UserMessageRequest request, WebSession session);
}
