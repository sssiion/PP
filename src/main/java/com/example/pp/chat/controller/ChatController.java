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
import java.util.UUID;
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatFlowService flow;

    @PostMapping
    public Mono<ServerMessageResponse> chat(@RequestBody UserMessageRequest req, WebSession session) {
        return flow.runPipeline(req, session);
    }

    @GetMapping("/result/{sessionId}")
    public Mono<List<RecommendedPlace>> result(@PathVariable String sessionId, WebSession session, PlaceStore placeStore) {
        @SuppressWarnings("unchecked")
        List<RecommendedPlace> inSession = (List<RecommendedPlace>) session.getAttributes().get(sessionId);
        if (inSession != null) return Mono.just(inSession);
        return placeStore.loadSessionResult(sessionId).map(list ->
                list.stream().map(s -> {
                    RecommendedPlace r = new RecommendedPlace();
                    r.setId(s.getId()); r.setName(s.getName());
                    r.setAddress(s.getAddress());
                    r.setLatitude(s.getLat()); r.setLongitude(s.getLon());
                    r.setCongestionLevel(s.getCongestionLevel());
                    r.setDistance(s.getDistanceKm());
                    return r;
                }).toList());
    }
}
