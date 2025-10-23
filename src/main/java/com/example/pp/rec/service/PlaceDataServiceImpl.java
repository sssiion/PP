package com.example.pp.rec.service;

import com.example.pp.rec.dto.Place;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlaceDataServiceImpl implements PlaceDataService {

    // (실제로는 @Autowired private PlaceRepository placeRepository; 가 필요)

    @Override
    public List<Place> getPlacesByLocation(String location, double radius) {
        // TODO: location(예: "강남역")을 위도/경도로 변환하는 로직 필요
        // TODO: repository.findWithinRadius(lat, lon, radius) 호출

        // --- Mock 데이터 (임시) ---
        System.out.println("[DB] " + location + " " + radius + "km 내 장소 검색");
        return List.of(
                new Place("스타벅스 강남점", "...", "카페", 37.49, 127.02),
                new Place("메가박스 강남", "...", "영화관", 37.49, 127.02),
                new Place("교보문고 강남", "...", "서점", 37.50, 127.02),
                new Place("블루보틀 역삼", "...", "카페", 37.50, 127.03)
        );
    }

    @Override
    public List<Place> filterByCategory(List<Place> places, String category) {
        System.out.println("[DB] 카테고리 필터링: " + category);
        return places.stream()
                .filter(place -> place.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }
}