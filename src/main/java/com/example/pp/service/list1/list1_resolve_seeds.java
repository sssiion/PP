package com.example.pp.service.list1;


import com.example.pp.entity.staion_info;
import com.example.pp.repository.SubwayStationRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.pp.service.list1.list1_models.normalizeName;


public class list1_resolve_seeds {
    private final SubwayStationRepository stationRepo;

    public list1_resolve_seeds(SubwayStationRepository stationRepo) { this.stationRepo = stationRepo; }
    // 숫자만 추출 → 선행 0 제거 → "n" 반환(예: "01호선"->"1", "4호선"->"4", "04"->"4")
    private String toNumericLine(String lineName) {
        if (lineName == null) return "";
        String digits = lineName.replaceAll("\\D+", "");   // 숫자만
        if (digits.isEmpty()) return "";
        // 선행 0 제거
        return digits.replaceFirst("^0+(?!$)", "");
    }

    // (핵심) 단일 역명 좌표 해석 + ‘역’ 접미사 제거 폴백
    private list1_models.Coord resolveOneWithFallback(String lineName, String stationName) {
        String numeric = toNumericLine(lineName); // "4호선"/"04" → "4"
        // 1차: 원본 역명으로 역+호선(유연) 매칭
        Optional<staion_info> opt = stationRepo.findFirstByStationAndFlexibleLine(stationName, numeric);
        if (opt.isEmpty()) {
            // 2차: 정규화 역명으로 재시도(괄호/공백 차이 보정)
            String norm = com.example.pp.service.list1.list1_models.normalizeName(stationName);
            opt = stationRepo.findFirstByStationAndFlexibleLine(norm, numeric);
        }
        if (opt.isEmpty() && stationName != null && stationName.endsWith("역")) {
            // 3차: ‘역’ 접미사 제거 후 재시도
            String noSuffix = stationName.substring(0, stationName.length()-1).trim();
            opt = stationRepo.findFirstByStationAndFlexibleLine(noSuffix, numeric);
            if (opt.isEmpty()) {
                String normNoSuffix = com.example.pp.service.list1.list1_models.normalizeName(noSuffix);
                opt = stationRepo.findFirstByStationAndFlexibleLine(normNoSuffix, numeric);
            }
        }
        return opt.map(s -> new list1_models.Coord(s.getLon(), s.getLat()))
                .orElse(new list1_models.Coord(0,0));
    }

    // (라인+역명) 우선 좌표 해석 → 실패 시 정규화/대소문자/원문 → ‘역’ 제거 폴백 → Seed dedup
    public List<list1_models.Seed> resolve(Map<String,String> normToOrig, Map<String,String> normToLine) {
        List<list1_models.Seed> seeds = normToOrig.entrySet().stream()
                .map(e -> {
                    String name = e.getValue();                         // 원문 역명(보정 포함)
                    String line = normToLine.getOrDefault(e.getKey(), ""); // 사람용 라인명(예: 1호선)
                    // 폴백 포함 단일 조회
                    list1_models.Coord c = resolveOneWithFallback(line, name);
                    return c;
                })
                .filter(c -> !(c.lon()==0 && c.lat()==0))
                .map(c -> new list1_models.Seed(c.lon(), c.lat()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                (list1_models.Seed s) -> s.lon() + "," + s.lat(),
                                Function.identity(),
                                (a,b)->a,
                                LinkedHashMap::new
                        ),
                        (Map<String, list1_models.Seed> m) -> new ArrayList<>(m.values())
                ));
        return seeds;
    }
}
