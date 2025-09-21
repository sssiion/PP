package com.example.pp.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenApiWrap<T>(Response<T> response) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Response<T>(Body<T> body) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Body<T>(Items<T> items) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Items<T>(List<T> item) {}
}
