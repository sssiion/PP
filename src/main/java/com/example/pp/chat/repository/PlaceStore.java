package com.example.pp.chat.repository;

import reactor.core.publisher.Mono;

import java.util.List;

public interface PlaceStore {
    Mono<Void> savePlacesBasic(String sessionId, List<PlaceBasic> places); // name, lat, lon
    Mono<Void> savePlacesWithCongestion(String sessionId, List<PlaceWithCongestion> places);
    Mono<List<PlaceWithCongestion>> loadSessionResult(String sessionId);
}