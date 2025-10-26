package com.example.pp.chat.service.ai;


import com.example.pp.chat.dto.ExternalSearchResult;
import com.example.pp.chat.dto.ParsedIntent;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface AiApiService {
    Mono<String> complete(String promptName, Map<String,Object> vars);
    Mono<JsonNode> completeAndValidate(String promptName, Map<String,Object> vars, String schemaName);

    Mono<ParsedIntent> parseIntentAndEntities(String userMessage, Object context);
    Mono<String> summarizePlacesAndBlogs(ParsedIntent intent, ExternalSearchResult result);
}