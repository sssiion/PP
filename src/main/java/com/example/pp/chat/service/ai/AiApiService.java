package com.example.pp.chat.service.ai;


import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface AiApiService {
    Mono<String> complete(String promptName, Map<String,Object> vars);
    Mono<JsonNode> completeAndValidate(String promptName, Map<String,Object> vars, String schemaName);
}
