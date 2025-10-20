package com.example.pp.data.service;

import com.example.pp.data.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public Map<String, Object> getDetailsForColumn(String category, List<String> ids, String column) {
        List<?> entities = switch (category.toLowerCase()) {
            case "accommodation" -> accommodationRepository.findAllById(ids);
            case "cultural_facilities" -> culturalFacilitiesRepository.findAllById(ids);
            case "festivals_performances_events" -> festivalsRepository.findAllById(ids);
            case "food" -> foodRepository.findAllById(ids);
            case "leisure_sports" -> leisureRepository.findAllById(ids);
            case "shopping" -> shoppingRepository.findAllById(ids);
            case "tourist_attraction" -> attractionRepository.findAllById(ids);
            case "travel_course" -> courseRepository.findAllById(ids);
            default -> List.of();
        };

        if (entities.isEmpty()) {
            return Map.of();
        }

        return entities.stream()
                .collect(Collectors.toMap(
                        entity -> (String) getFieldValue(entity, "Id"),
                        entity -> getFieldValue(entity, column)
                ));
    }

    private Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field;
            try {
                // First, try to get the field directly (assuming case match)
                field = obj.getClass().getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // If not found, try to find it case-insensitively
                field = findFieldCaseInsensitive(obj.getClass(), fieldName);
            }
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // For simplicity, return null if field doesn't exist or is inaccessible
            return null;
        }
    }

    private Field findFieldCaseInsensitive(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equalsIgnoreCase(fieldName)) {
                return field;
            }
        }
        // Check superclasses as well, if necessary
        if (clazz.getSuperclass() != null) {
            return findFieldCaseInsensitive(clazz.getSuperclass(), fieldName);
        }
        throw new NoSuchFieldException("No such field: " + fieldName + " in class " + clazz.getName());
    }
}
