package com.example.pp.chat.util;




import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Component;

import javax.xml.validation.Schema;
import java.io.StringReader;

@Component
public class JsonSchemaValidator {
    private final ObjectMapper om = new ObjectMapper();

    // schemaJson 문자열은 더 이상 사용하지 않고, 필수 키/타입만 간단 체크
    public void validate(String unusedSchemaJson, String payloadJson) {
        try {
            JsonNode root = om.readTree(payloadJson);
            // 예: 공통 출력 포맷 강제
            // placeSummary: string, reasonsToGo: array, reasonsNotToGo: array
            if (!root.has("placeSummary") || !root.path("placeSummary").isTextual()) {
                throw new IllegalArgumentException("placeSummary must be string");
            }
            if (!root.has("reasonsToGo") || !root.path("reasonsToGo").isArray()) {
                throw new IllegalArgumentException("reasonsToGo must be array");
            }
            if (!root.has("reasonsNotToGo") || !root.path("reasonsNotToGo").isArray()) {
                throw new IllegalArgumentException("reasonsNotToGo must be array");
            }
            // 추가로 필요하면 길이/요소 타입 점검
        } catch (Exception e) {
            throw new IllegalStateException("Simple JSON validation failed", e);
        }
    }
}
