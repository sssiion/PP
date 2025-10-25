package com.example.pp.rec.controller;


import com.example.pp.auth.SessionUser;
import com.example.pp.rec.dto.ServerMessageResponse;
import com.example.pp.rec.dto.UserMessageRequest;
import com.example.pp.rec.entity.ChatContext;
import com.example.pp.rec.repository.ContextRepository;
import com.example.pp.rec.service.ChatOrchestrationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;


@RestController
public class ChatController {

    private final ChatOrchestrationService orchestrationService;
    private final ContextRepository contextRepository;

    public ChatController(ChatOrchestrationService orchestrationService, ContextRepository contextRepository) {
        this.orchestrationService = orchestrationService;
        this.contextRepository = contextRepository;
    }

    @PostMapping("/chat")
    public Mono<ServerMessageResponse> handleChat(@RequestBody UserMessageRequest request, WebSession session, @SessionAttribute(name = "user", required = false) SessionUser sessionUser) { // WebSession, SessionUser 추가

        // 로그인한 사용자의 경우, 요청의 sessionId를 사용자의 providerId로 설정
        if (sessionUser != null) {
            request.setSessionId(sessionUser.getProviderId());
        }

        // (2) [JPA 읽기] - 비동기 래핑
        // JPA(DB) 작업은 블로킹 작업이므로, 별도 스레드에서 실행하도록 격리
        Mono<ChatContext> contextMono = Mono.fromCallable(() ->
                contextRepository.findByUserId(request.getUserId())
                        .orElse(new ChatContext(request.getUserId())) // 없으면 새로 생성
        ).subscribeOn(Schedulers.boundedElastic()); // JPA용 스케줄러 사용


        // (3) 비동기 체인 시작
        return contextMono
                .flatMap(context ->
                        // (4) 핵심 비동기 로직 호출 (request, session 전달)
                        orchestrationService.processMessage(context, request, session)
                                .flatMap(response -> // (5) 로직이 끝나면

                                        // (6) [JPA 쓰기] - 비동기 래핑
                                        // context를 DB에 저장하는 것도 블로킹 작업이므로 격리
                                        Mono.fromCallable(() -> {
                                            context.setUpdatedAt(Instant.now());
                                            contextRepository.save(context);
                                            return response; // 최종 응답(response)을 다음 체인으로 전달
                                        }).subscribeOn(Schedulers.boundedElastic())
                                )
                );
    }
}