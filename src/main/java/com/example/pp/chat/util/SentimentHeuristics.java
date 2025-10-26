package com.example.pp.chat.util;

import java.util.List;

public class SentimentHeuristics {
    private static final String[] POS = {"맛있", "깨끗", "친절", "뷰 좋", "추천", "가성비", "조용", "여유"};
    private static final String[] NEG = {"불친절", "시끄럽", "비싸", "줄 길", "붐빔", "대기", "불편"};

    public static double blogSignalScore(List<String> snippets){
        if (snippets == null || snippets.isEmpty()) return 0.5;
        int pos=0, neg=0;
        for(String s: snippets){
            String t = s==null? "": s;
            for (String p: POS) if (t.contains(p)) pos++;
            for (String n: NEG) if (t.contains(n)) neg++;
        }
        int total = Math.max(1, pos+neg);
        double raw = (pos - neg) / (double) total; // -1..1
        return Math.max(0.0, Math.min(1.0, 0.5 + raw/2.0)); // 0..1 정규화
    }
}
