package com.example.pp.rec.service;

import com.example.pp.rec.dto.Place;

import java.util.List;

public interface PlaceDataService {
    List<Place> getPlacesByLocation(String location, double radius);
    List<Place> filterByCategory(List<Place> places, String category);
}
