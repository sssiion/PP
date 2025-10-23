package com.example.pp.rec.dto;
// 사용자가 서버에 채팅 메시지를 보낼 때 사용하는 DTO
public class UserMessageRequest {
    private String userId;
    private String message;

    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}