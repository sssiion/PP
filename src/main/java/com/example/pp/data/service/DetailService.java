package com.example.pp.data.service;

import com.example.pp.data.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DetailService {

    private final accommodationRepository accommodationRepository;
    private final cultural_facilitiesRepository culturalFacilitiesRepository;
    private final Festivals_Performances_EventsRepository festivalsRepository;
    private final foodRepository foodRepository;
    private final leisure_sportsRepository leisureRepository;
    private final shoppingRepository shoppingRepository;
    private final tourist_attractionRepository attractionRepository;
    private final travel_courseRepository courseRepository;

    public Optional<?> getDetail(String category, String id) {
        return switch (category.toLowerCase()) {
            case "accommodation" -> accommodationRepository.findById(id);
            case "cultural_facilities" -> culturalFacilitiesRepository.findById(id);
            case "festivals_performances_events" -> festivalsRepository.findById(id);
            case "food" -> foodRepository.findById(id);
            case "leisure_sports" -> leisureRepository.findById(id);
            case "shopping" -> shoppingRepository.findById(id);
            case "tourist_attraction" -> attractionRepository.findById(id);
            case "travel_course" -> courseRepository.findById(id);
            default -> Optional.empty();
        };
    }
}
