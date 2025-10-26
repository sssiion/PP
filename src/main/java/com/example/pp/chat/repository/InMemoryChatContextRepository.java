package com.example.pp.chat.repository;


import com.example.pp.chat.entity.ChatContext;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Repository
public class InMemoryChatContextRepository { // 이름이 Repository지만 간단 Bean
    private final Map<String, ChatContext> store = new ConcurrentHashMap<>();

    public Mono<ChatContext> findById(String id) {
        return Mono.justOrEmpty(store.get(id));
    }
    public Mono<ChatContext> save(ChatContext c) {
        store.put(c.getSessionId(), c);
        return Mono.just(c);
    }
}