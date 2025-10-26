package com.example.pp.chat.mapper;

import com.example.pp.chat.dto.RecommendedPlace;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlaceMapper {

    // Kakao keyword/category 검색 결과 → RecommendedPlace
    public static List<RecommendedPlace> fromKakaoDocuments(List<Map<String,Object>> docs) {
        return docs.stream().map(doc -> {
            RecommendedPlace p = new RecommendedPlace();
            p.setId(String.valueOf(doc.getOrDefault("id","")));
            p.setName((String) doc.getOrDefault("place_name",""));
            p.setAddress((String) doc.getOrDefault("road_address_name",
                    (String) doc.getOrDefault("address_name","")));
            try {
                p.setLatitude(Double.parseDouble(String.valueOf(doc.getOrDefault("y","0"))));
                p.setLongitude(Double.parseDouble(String.valueOf(doc.getOrDefault("x","0"))));
            } catch (Exception ignore) { p.setLatitude(0); p.setLongitude(0); }
            p.setCategory((String) doc.getOrDefault("category_name",""));
            // Kakao distance는 center 지정 시 m 단위 문자열로 제공 가능
            double km = 0.0;
            try { km = Double.parseDouble(String.valueOf(doc.getOrDefault("distance","0"))) / 1000.0; } catch (Exception ignore) {}
            p.setDistance(km);
            p.setCongestionLevel("정보없음");
            p.setOriginalOtherInfo(""); // 필요 시 상세 Raw 보관
            return p;
        }).collect(Collectors.toList());
    }

    // Naver local 검색 → 보조 매핑(좌표 부재 가능, 거리 미계산)
    public static List<RecommendedPlace> fromNaverLocal(List<Map<String,Object>> items) {
        return items.stream().map(it -> {
            RecommendedPlace p = new RecommendedPlace();
            p.setId(String.valueOf(it.getOrDefault("link","")));
            p.setName(stripTags((String) it.getOrDefault("title","")));
            p.setAddress((String) it.getOrDefault("address",""));
            p.setCategory((String) it.getOrDefault("category",""));
            p.setLatitude(0.0); p.setLongitude(0.0); // 좌표는 별도 지오코딩 필요
            p.setDistance(0.0);
            p.setCongestionLevel("정보없음");
            p.setOriginalOtherInfo("");
            return p;
        }).collect(Collectors.toList());
    }

    private static String stripTags(String s) {
        if (s == null) return "";
        return s.replaceAll("<.*?>", "");
    }
}
