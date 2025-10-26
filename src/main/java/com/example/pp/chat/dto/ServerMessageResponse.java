package com.example.pp.chat.dto;



import lombok.*;
import java.util.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ServerMessageResponse {
    private String type; // "OK", "TEXT", "ERROR"
    private String message;
    private String replyText;
    private List<String> pros;
    private List<String> cons;
    private List<RecommendedPlace> items;

    public static ServerMessageResponse text(String reply){
        ServerMessageResponse r = new ServerMessageResponse("TEXT", reply, reply, List.of(), List.of(), List.of());
        return r;
    }
    public static ServerMessageResponse ok(String reply, List<String> pros, List<String> cons, List<RecommendedPlace> items){
        ServerMessageResponse r = new ServerMessageResponse("OK", reply, reply, pros!=null?pros:List.of(), cons!=null?cons:List.of(), items!=null?items:List.of());
        return r;
    }
    public static ServerMessageResponse from(List<RecommendedPlace> items){
        String reply="추천 결과를 정리했어요.";
        List<String> pros=List.of(), cons=List.of();
        if (items!=null && !items.isEmpty() && items.get(0).getData()!=null){
            var d = items.get(0).getData();
            if (d.getPlaceSummary()!=null) reply=d.getPlaceSummary();
            pros = d.getReasonsToGo()!=null ? d.getReasonsToGo() : List.of();
            cons = d.getReasonsNotToGo()!=null ? d.getReasonsNotToGo() : List.of();
        }
        return ok(reply, pros, cons, items!=null?items:List.of());
    }
    public static ServerMessageResponse error(String msg){
        ServerMessageResponse r = new ServerMessageResponse("ERROR", msg, msg, List.of(), List.of(), List.of());
        return r;
    }
}

