package com.example.pp.chat.entity;

import com.example.pp.chat.converter.MapStringStringConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;


@Data
public class ChatContext {
    private String sessionId;
    private String state;
    private String lastPlaceName;
    private String lastCategory;
    private Double currentSearchLat;
    private Double currentSearchLon;
    private String congestionPreference;
    private String lastIntent;
    private Map<String,String> preferenceKeywords;
    private Map<String,Boolean> flags = new HashMap<>();
}