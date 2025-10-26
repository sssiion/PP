package com.example.pp.chat.service.rank;

import com.example.pp.chat.dto.RecommendedPlace;
import com.example.pp.chat.util.SentimentHeuristics;

import java.util.List;
import java.util.Map;

public class RuleScorer {

    private static final Map<String, Double> CONGESTION_SCORE = Map.of(
            "여유", 1.0, "보통", 0.8, "약간 붐빔", 0.5, "붐빔", 0.2, "정보없음", 0.6
    );

    public static List<RecommendedPlace> scoreAndSort(List<RecommendedPlace> input, Map<String, List<String>> placeSnippets){
        return input.stream().sorted((a,b) -> {
            double sa = score(a, placeSnippets);
            double sb = score(b, placeSnippets);
            return -Double.compare(sa, sb);
        }).toList();
    }

    private static double score(RecommendedPlace p, Map<String, List<String>> snippets){
        double cong = CONGESTION_SCORE.getOrDefault(p.getCongestionLevel(), 0.6);
        double dist = p.getDistance() <= 0 ? 0.5 : Math.max(0.0, 1.0 - Math.min(1.0, p.getDistance()/10.0));
        double blog = SentimentHeuristics.blogSignalScore(snippets.getOrDefault(p.getId(), List.of()));
        return 0.45*cong + 0.25*dist + 0.30*blog;
    }
}
