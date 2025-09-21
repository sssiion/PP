package com.example.pp.service;

import com.example.pp.dto.LineStationsResponse;
import com.example.pp.entity.SubwayStation;
import com.example.pp.repository.SubwayStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StationCoordinateResolver {
    private final SubwayStationRepository repo;

    public Optional<Coord> resolveByCodeOrName(String stationCode, String stationName) {
        return repo.findOneByStationId(stationCode)
                .or(() -> repo.findOneByStationName(normalize(stationName)))
                .map(s -> new Coord(s.getLat(), s.getLon()));
    }

    public List<ResolvedStop> resolveForLineStops(List<LineStationsResponse.Row> stops) {
        List<ResolvedStop> out = new ArrayList<>();
        for (var r : stops) {
            var coord = resolveByCodeOrName(r.stationCode(), r.stationNameKo());
            coord.ifPresent(c -> out.add(new ResolvedStop(r.stationCode(), r.stationNameKo(), c.lat, c.lon)));
        }
        return out;
    }

    private String normalize(String name) {
        // 부역명 제거 등 필요 시 정규화 로직 추가
        return name.replace("(", " ").replace(")", " ").trim();
    }

    public record Coord(double lat, double lon) {}
    public record ResolvedStop(String stationCode, String stationName, double lat, double lon) {}
}
