package com.example.pp.rec.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class TmapService {

    private final WebClient tmapWebClient;
    private final String tmapApiKey;

    public TmapService(@Qualifier("tmapWebClient") WebClient tmapWebClient,
                       @Value("${tmap.api.key}") String tmapApiKey) {
        this.tmapWebClient = tmapWebClient;
        this.tmapApiKey = tmapApiKey;
    }

    public Mono<String> getRoute(double startX, double startY, double endX, double endY, String searchOption) {
        return tmapWebClient.post()
                .uri("/tmap/routes/pedestrian?version=1")
                .header("appKey", tmapApiKey)
                .body(BodyInserters.fromFormData("startX", String.valueOf(startX))
                        .with("startY", String.valueOf(startY))
                        .with("endX", String.valueOf(endX))
                        .with("endY", String.valueOf(endY))
                        .with("startName", "start")
                        .with("endName", "end")
                        .with("searchOption", searchOption))
                .retrieve()
                .bodyToMono(String.class);
    }
}
