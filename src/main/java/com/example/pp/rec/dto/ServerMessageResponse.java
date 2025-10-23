package com.example.pp.rec.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 서버가 사용자에게 응답할 때 사용하는 DTO
@Getter
@Setter
public class ServerMessageResponse {
    private String replyText;
    private List<String> pros;
    private List<String> cons;

    // --- 생성자 ---
    public ServerMessageResponse(String replyText) {
        this.replyText = replyText;
    }

    public ServerMessageResponse(String replyText, List<String> pros, List<String> cons) {
        this.replyText = replyText;
        this.pros = pros;
        this.cons = cons;
    }

    public ServerMessageResponse() { }

    // --- Getters and Setters ---
    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }
    public List<String> getPros() { return pros; }
    public void setPros(List<String> pros) { this.pros = pros; }
    public List<String> getCons() { return cons; }
    public void setCons(List<String> cons) { this.cons = cons; }
}