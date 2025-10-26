package com.example.pp.chat.service.rank;



import com.example.pp.chat.dto.RecommendedPlace;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RankingService {

    private static final Map<String,Integer> CONGESTION_ORDER = Map.of(
            "여유", 1, "보통", 2, "약간 붐빔", 3, "붐빔", 4, "정보없음", 5
    );

    public List<RecommendedPlace> dedup(List<RecommendedPlace> input) {
        Map<String, RecommendedPlace> map = new LinkedHashMap<>();
        for (RecommendedPlace p : input) {
            String key = normalize(p.getName()) + "|" + gridKey(p.getLatitude(), p.getLongitude(), 3);
            map.putIfAbsent(key, p);
        }
        return new ArrayList<>(map.values());
    }

    public List<RecommendedPlace> sortByCongestionThenDistance(List<RecommendedPlace> input) {
        return input.stream()
                .sorted(Comparator
                        .comparing((RecommendedPlace p) -> CONGESTION_ORDER.getOrDefault(p.getCongestionLevel(), 5))
                        .thenComparingDouble(RecommendedPlace::getDistance)
                )
                .collect(Collectors.toList());
    }

    public List<RecommendedPlace> topN(List<RecommendedPlace> input, int n) {
        if (input.size() <= n) return input;
        return input.subList(0, n);
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private String gridKey(double lat, double lon, int precision) {
        long y = Math.round(lat * Math.pow(10, precision));
        long x = Math.round(lon * Math.pow(10, precision));
        return y + "," + x;
    }
}
