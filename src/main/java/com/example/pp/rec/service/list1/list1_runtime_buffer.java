package com.example.pp.rec.service.list1;


import com.example.pp.rec.dto.TourPoiResponse;
import com.example.pp.rec.entity.staion_info;
import com.example.pp.rec.service.list1.list1_models.FoundLine;
import com.example.pp.rec.service.list1.list1_models.LineCtx;
import com.example.pp.rec.service.list1.list1_models.Seed;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// 실행 중 임시 보관소(프로그램 종료 시 메모리에서 소멸)
// // 동시성 안전 컬렉션을 사용하고, 마지막 실행 결과만 유지한다.
@Component
public class list1_runtime_buffer {

    // 1) 주변역
    private final List<staion_info> lastNearbyStations = new CopyOnWriteArrayList<>();

    // 2) DB 선별 + API 매핑 결과(FoundLine)
    private final List<FoundLine> lastFoundLines = new CopyOnWriteArrayList<>();

    // 3) 라인 정차역 컨텍스트
    private final Map<String, LineCtx> lastLineCtxMap = new ConcurrentHashMap<>();

    // 4) 다음/다다음역 이름/라인 매핑
    private final Map<String, String> lastNextStationsNameMap = new ConcurrentHashMap<>();
    private final Map<String, String> lastNextStationsLineMap = new ConcurrentHashMap<>();

    // 5) 좌표 시드
    private final List<Seed> lastSeeds = new CopyOnWriteArrayList<>();

    // 6) 관광지 응답(원본 Item, contentid 키)
    private final Map<String, TourPoiResponse.Item> lastTourItems = new ConcurrentHashMap<>();

    // setters (스냅샷 통째로 교체)
    public void setNearbyStations(List<staion_info> v) { lastNearbyStations.clear(); if (v!=null) lastNearbyStations.addAll(v); }
    public void setFoundLines(List<FoundLine> v) { lastFoundLines.clear(); if (v!=null) lastFoundLines.addAll(v); }
    public void setLineCtxMap(Map<String, LineCtx> m) { lastLineCtxMap.clear(); if (m!=null) lastLineCtxMap.putAll(m); }
    public void setNextStations(Map<String,String> nameMap, Map<String,String> lineMap) {
        lastNextStationsNameMap.clear(); lastNextStationsLineMap.clear();
        if (nameMap!=null) lastNextStationsNameMap.putAll(nameMap);
        if (lineMap!=null) lastNextStationsLineMap.putAll(lineMap);
    }
    public void setSeeds(List<Seed> v) { lastSeeds.clear(); if (v!=null) lastSeeds.addAll(v); }
    public void setTourItems(Map<String, TourPoiResponse.Item> m) { lastTourItems.clear(); if (m!=null) lastTourItems.putAll(m); }

    // getters (디버깅/재사용)
    public List<staion_info> getNearbyStations() { return List.copyOf(lastNearbyStations); }
    public List<FoundLine> getFoundLines() { return List.copyOf(lastFoundLines); }
    public Map<String, LineCtx> getLineCtxMap() { return Map.copyOf(lastLineCtxMap); }
    public Map<String, String> getNextStationsNameMap() { return Map.copyOf(lastNextStationsNameMap); }
    public Map<String, String> getNextStationsLineMap() { return Map.copyOf(lastNextStationsLineMap); }
    public List<Seed> getSeeds() { return List.copyOf(lastSeeds); }
    public Map<String, TourPoiResponse.Item> getTourItems() { return Map.copyOf(lastTourItems); }

    // 전체 초기화(원하면 호출)
    public void clearAll() {
        lastNearbyStations.clear();
        lastFoundLines.clear();
        lastLineCtxMap.clear();
        lastNextStationsNameMap.clear();
        lastNextStationsLineMap.clear();
        lastSeeds.clear();
        lastTourItems.clear();
    }
}
