package com.example.pp.chat.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserMessageRequest {
    private String message;
    private String sessionId;
    private Double lat;
    private Double lon;
    private LocalTime time;
}
