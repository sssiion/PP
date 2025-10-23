package com.example.pp.rec.service;

import com.example.pp.rec.dto.ServerMessageResponse;
import com.example.pp.rec.entity.ChatContext;


public interface ChatOrchestrationService {
    ServerMessageResponse processMessage(ChatContext context, String userMessage);
}
