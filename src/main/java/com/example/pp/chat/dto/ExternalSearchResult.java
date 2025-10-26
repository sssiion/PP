package com.example.pp.chat.dto;


import lombok.Data;
import java.util.*;

@Data
public class ExternalSearchResult {
    private List<RecommendedPlace> places = new ArrayList<>();
    private List<BlogReview> blogs = new ArrayList<>();
}
