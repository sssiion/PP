package com.example.pp.service.list1;

import java.util.*;

import static com.example.pp.service.list1.list1_models.ensureStationSuffix;
import static com.example.pp.service.list1.list1_models.normalizeName;

public class list1_next_stations {
    // 다음/다다음역 선택(정방향 전진, 접미사 보정 포함)
    public static AbstractMap.SimpleEntry<Map<String,String>, Map<String,String>>
    pick(List<list1_models.FoundLine> found, Map<String, list1_models.LineCtx> lineCtxMap) {
        LinkedHashMap<String,String> normToOrig = new LinkedHashMap<>();
        LinkedHashMap<String,String> normToLine = new LinkedHashMap<>();

        for (list1_models.FoundLine f : found) {
            list1_models.LineCtx ctx = lineCtxMap.get(f.lineParam());
            if (ctx == null || ctx.stops().isEmpty()) continue;

            String base = ensureStationSuffix(f.stationNameKo());
            Integer idx = ctx.nameToIdx().get(normalizeName(base));
            if (idx == null) continue;

            int i1 = Math.min(idx+1, ctx.stops().size()-1);
            int i2 = Math.min(idx+2, ctx.stops().size()-1);
            String n1 = ensureStationSuffix(ctx.stops().get(i1).stationNameKo());
            String n2 = ensureStationSuffix(ctx.stops().get(i2).stationNameKo());

            String k1 = normalizeName(n1);
            String k2 = normalizeName(n2);

            normToOrig.putIfAbsent(k1, n1);
            normToOrig.putIfAbsent(k2, n2);
            normToLine.putIfAbsent(k1, f.humanLineName());
            normToLine.putIfAbsent(k2, f.humanLineName());
        }
        return new AbstractMap.SimpleEntry<>(normToOrig, normToLine);
    }
}
