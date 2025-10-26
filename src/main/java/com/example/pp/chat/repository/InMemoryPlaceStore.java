package com.example.pp.chat.repository;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryPlaceStore implements PlaceStore {
    private final Map<String, List<PlaceBasic>> basicBySession = new ConcurrentHashMap<>();
    private final Map<String, List<PlaceWithCongestion>> resultBySession = new ConcurrentHashMap<>();

    public Mono<Void> savePlacesBasic(String sessionId, List<PlaceBasic> places) {
        basicBySession.put(sessionId, places);
        return Mono.empty();
    }
    public Mono<Void> savePlacesWithCongestion(String sessionId, List<PlaceWithCongestion> places) {
        resultBySession.put(sessionId, places);
        return Mono.empty();
    }
    public Mono<List<PlaceWithCongestion>> loadSessionResult(String sessionId) {
        return Mono.justOrEmpty(resultBySession.getOrDefault(sessionId, List.of()));
    }
}
