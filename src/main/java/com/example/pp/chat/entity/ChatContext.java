package com.example.pp.chat.entity;

import com.example.pp.chat.converter.MapStringStringConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Map;

@Entity
@Table(name = "chat_context")
@Data
public class ChatContext {
    @Id
    private String sessionId;

    private String state; // ChatState.name() 저장

    private String congestionPreference; // ex) 여유/보통/약간 붐빔/붐빔

    @Convert(converter = MapStringStringConverter.class)
    private Map<String, String> preferenceKeywords;

    private Double currentSearchLat;
    private Double currentSearchLon;

    private String promptVersion; // 프롬프트 버저닝
}
