package com.example.pp.data.service;

import com.example.pp.data.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class NearbyPostgisService {

        private final accommodationRepository accommodationRepository;
        private final cultural_facilitiesRepository culturalFacilitiesRepository;
        private final Festivals_Performances_EventsRepository festivalsRepository;
        private final foodRepository foodRepository;
        private final leisure_sportsRepository leisureRepository;
        private final shoppingRepository shoppingRepository;
        private final tourist_attractionRepository attractionRepository;
        private final travel_courseRepository courseRepository;



    // 단일 시드로부터 8개 엔티티를 직접 모아 반환
    public Map<String, List<?>> findAllEntitiesWithinRadius(double lat, double lon, double radiusMeters) {
        Map<String, List<?>> out = new LinkedHashMap<>();
        out.put("accommodation", accommodationRepository.findEntitiesWithinRadius(lat, lon, radiusMeters));
        out.put("cultural_facilities", culturalFacilitiesRepository.findEntitiesWithinRadius(lat, lon, radiusMeters));
        out.put("festivals_performances_events", festivalsRepository.findEntitiesWithinRadius(lat, lon, radiusMeters));
        out.put("food", foodRepository.findEntitiesWithinRadius(lat, lon, radiusMeters));
        out.put("leisure_sports", leisureRepository.findEntitiesWithinRadius(lat, lon, radiusMeters));
        out.put("shopping", shoppingRepository.findEntitiesWithinRadius(lat, lon, radiusMeters));
        out.put("tourist_attraction", attractionRepository.findEntitiesWithinRadius(lat, lon, radiusMeters));
        out.put("travel_course", courseRepository.findEntitiesWithinRadius(lat, lon, radiusMeters));
        return out;
        }

        private static List<NearbyDestination> map(String table, List<NearbyRow> rows) {
            List<NearbyDestination> list = new ArrayList<>();
            for (NearbyRow r : rows) {
                list.add(new NearbyDestination(
                        table, r.getId(), r.getName(),
                        r.getLat(), r.getLon(),
                        r.getDistanceMeters()
                ));
            }
            return list;
        }

        public record NearbyDestination(
                String sourceTable,
                String id,
                String name,
                Double lat,
                Double lon,
                Double distanceMeters
        ) {}
    }
