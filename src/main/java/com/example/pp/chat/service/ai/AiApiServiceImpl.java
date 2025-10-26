package com.example.pp.chat.service.ai;


import com.example.pp.chat.dto.ExternalSearchResult;
import com.example.pp.chat.dto.ParsedIntent;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    @Override
    public Mono<ParsedIntent> parseIntentAndEntities(String userMessage, Object context) {
        ParsedIntent intent = new ParsedIntent();
        // 예: 반경 추출 (단위 km, m 모두 대응)
        Pattern radiusPat = Pattern.compile("반경 ?([0-9]+)\\s*(km|m)");
        Matcher m = radiusPat.matcher(userMessage);
        if (m.find()) {
            int r = Integer.parseInt(m.group(1));
            intent.setRadius(m.group(2).equals("km") ? r * 1000 : r);
        }
        // 장소명 추출 - 간단 정규식
        Pattern placePat = Pattern.compile("([가-힣A-Za-z0-9]+(역|동|구|시|군|면))");
        Matcher pm = placePat.matcher(userMessage);
        if (pm.find()) {
            intent.setPlaceName(pm.group(1));
        }
        // 키워드 예시 추출
        List<String> keywords = new ArrayList<>();
        for (String k : List.of("카페","맛집","조용한","아기자기","서점","공원")) {
            if (userMessage.contains(k)) keywords.add(k);
        }
        intent.setKeywords(keywords);

        // ready 판정: 장소명 또는 키워드가 있으면 true
        intent.setReady((intent.getPlaceName() != null && !intent.getPlaceName().isBlank()) || !keywords.isEmpty());

        if (!intent.isReady()) intent.setAskWhatIsMissing("원하시는 장소나 조건을 좀 더 알려주세요. 예) ‘홍대역 조용한 카페’");
        return Mono.just(intent);
    }

    @Override
    public Mono<String> summarizePlacesAndBlogs(ParsedIntent intent, ExternalSearchResult result) {
        // 카카오 잠소 + 네이버 블로그 리스트를 AI 프롬프트에 모두 넣고 요약 생성
        StringBuilder sb = new StringBuilder();
        sb.append(intent.getPlaceName()).append(" 주변 추천: ").append(result.getPlaces().size()).append(" 곳\n");
        for (var p: result.getPlaces()) sb.append("- ").append(p.getName()).append(": ").append(p.getAddress()).append("\n");
        if (result.getBlogs()!=null && !result.getBlogs().isEmpty()) {
            sb.append("관련 블로그 리뷰 Top 3:\n");
            for (int i=0; i<Math.min(result.getBlogs().size(),3); i++) {
                var rev = result.getBlogs().get(i);
                sb.append("* ").append(rev.getTitle()).append(" - ").append(rev.getDescription()).append("\n");
            }
        }
        return Mono.just(sb.toString());
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
