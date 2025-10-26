package com.example.pp.chat.service.ai;


import com.example.pp.chat.util.JsonSchemaValidator;
import com.example.pp.chat.util.PromptLoader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class AiApiServiceImpl implements AiApiService {

    // Bean 주입 권장: config.HttpClientConfig에서 baseUrl/헤더 세팅 [web:101]
    private final WebClient geminiWebClient;

    // 모델명은 프로퍼티로 뺄 수 있음: @Value("${gemini.model:gemini-2.0-flash}")
    private String model = "gemini-2.0-flash";

    private final PromptLoader promptLoader;
    private final JsonSchemaValidator schemaValidator;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${gemini.api-key:}") // 비어 있으면 아래에서 System.getenv fallback
    private String apiKeyProp;

    private String apiKey() {
        String k = (apiKeyProp == null || apiKeyProp.isBlank()) ? System.getenv("GEMINI_API_KEY") : apiKeyProp;
        if (k == null || k.isBlank()) throw new IllegalStateException("GEMINI_API_KEY is missing");
        return k;
    }

    public Mono<Map> generateContent(String model, String prompt) {
        Map<String,Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );
        return geminiWebClient.post()
                .uri(uri -> uri.path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", apiKey())
                        .build(model))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class);
    }

    @Override
    public Mono<String> complete(String promptName, Map<String, Object> vars) {
        String prompt = promptLoader.loadPrompt(promptName);
        String content = TemplateEngine.fill(prompt, vars);
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", content))
                ))
        );
        return geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", apiKey())
                        .build(model))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(m -> {
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) m.getOrDefault("candidates", List.of());
                    if (candidates.isEmpty()) return "";
                    Map<String, Object> contentMap = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.getOrDefault("parts", List.of());
                    return parts.isEmpty() ? "" : String.valueOf(parts.get(0).getOrDefault("text", ""));
                });
    }

    @Override
    public Mono<JsonNode> completeAndValidate(String promptName, Map<String, Object> vars, String schemaName) {
        String schema = promptLoader.loadSchema(schemaName);
        return complete(promptName, vars).map(raw -> {
            schemaValidator.validate(schema, raw);
            try {
                return om.readTree(raw);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

final class TemplateEngine {
    static String fill(String tmpl, Map<String,Object> vars) {
        String out = tmpl;
        for (var e: vars.entrySet()) {
            out = out.replace("${"+e.getKey()+"}", String.valueOf(e.getValue()));
        }
        return out;
    }
}
