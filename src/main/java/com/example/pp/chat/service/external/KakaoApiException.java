package com.example.pp.chat.service.external;

public class KakaoApiException extends RuntimeException {
    private final int status;
    private final String body;

    public KakaoApiException(int status, String body) {
        super("Kakao API error " + status + " - " + body);
        this.status = status;
        this.body = body;
    }
    public int getStatus() { return status; }
    public String getBody() { return body; }
}

