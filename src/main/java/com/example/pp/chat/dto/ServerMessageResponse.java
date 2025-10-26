package com.example.pp.chat.dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.ok;

@Data
@AllArgsConstructor
public class ServerMessageResponse {
    private String type;                 // e.g., "OK" | "ERROR" | "TEXT"
    private String message;              // 기존 메시지(호환 유지)
    private String replyText;            // 프런트가 바로 쓰는 텍스트
    private List<String> pros;           // 장점 (선택)
    private List<String> cons;           // 단점 (선택)
    private List<RecommendedPlace> items; // 추천 목록 (선택)

    public ServerMessageResponse(){}
    public static ServerMessageResponse ok(String reply, List<String> pros, List<String> cons, List<RecommendedPlace> items){
        ServerMessageResponse r = new ServerMessageResponse();
        r.type = "OK";
        r.replyText = Optional.ofNullable(reply).orElse("");
        r.message = r.replyText;
        r.pros = Optional.ofNullable(pros).orElse(List.of());
        r.cons = Optional.ofNullable(cons).orElse(List.of());
        r.items = Optional.ofNullable(items).orElse(List.of());
        return r;
    }
    public static ServerMessageResponse text(String reply) {
        ServerMessageResponse r = new ServerMessageResponse();
        r.type = "TEXT";
        r.replyText = Optional.ofNullable(reply).orElse("");
        r.pros = List.of();
        r.cons = List.of();
        r.items = List.of();
        r.message = r.replyText; // 호환
        return r;
    }
    public static ServerMessageResponse from(List<RecommendedPlace> items){
        // items[0].data.placeSummary를 replyText로 사용
        String reply = "추천 결과를 정리했어요.";
        List<String> pros = List.of();
        List<String> cons = List.of();
        if (items != null && !items.isEmpty() && items.get(0).getData()!=null){
            var d = items.get(0).getData();
            if (d.getPlaceSummary()!=null) reply = d.getPlaceSummary();
            pros = Optional.ofNullable(d.getReasonsToGo()).orElse(List.of());
            cons = Optional.ofNullable(d.getReasonsNotToGo()).orElse(List.of());
        }
        return ok(reply, pros, cons, Optional.ofNullable(items).orElse(List.of()));
    }

    public static ServerMessageResponse error(String msg){
        ServerMessageResponse r = new ServerMessageResponse();
        r.type = "ERROR";
        r.replyText = Optional.ofNullable(msg).orElse("오류가 발생했습니다.");
        r.message = r.replyText;
        r.pros = List.of();
        r.cons = List.of();
        r.items = List.of();
        return r;
    }

}
