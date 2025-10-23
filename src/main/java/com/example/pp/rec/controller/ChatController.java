package com.example.pp.rec.controller;


import com.example.pp.rec.dto.ServerMessageResponse;
import com.example.pp.rec.dto.UserMessageRequest;
import com.example.pp.rec.entity.ChatContext;
import com.example.pp.rec.repository.ContextRepository;
import com.example.pp.rec.service.ChatOrchestrationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class ChatController {

    private final ChatOrchestrationService orchestrationService;
    private final ContextRepository contextRepository;

    public ChatController(ChatOrchestrationService orchestrationService, ContextRepository contextRepository) {
        this.orchestrationService = orchestrationService;
        this.contextRepository = contextRepository;
    }

    @PostMapping("/chat")
    public ServerMessageResponse handleChat(@RequestBody UserMessageRequest request) {

        // 1. 사용자 ID로 기존 ChatContext 로드 (없으면 새로 생성)
        ChatContext context = contextRepository.findByUserId(request.getUserId())
                .orElse(new ChatContext(request.getUserId()));

        // 2. 핵심 로직(자동화) 실행
        ServerMessageResponse response = orchestrationService.processMessage(context, request.getMessage());

        // 3. 변경된 Context (상태, 요약본 등) 저장
        contextRepository.save(context);

        // 4. 사용자에게 응답
        return response;
    }
}