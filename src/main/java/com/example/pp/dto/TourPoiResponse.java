package com.example.pp.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourPoiResponse(
        @JsonProperty("response") Resp response
){
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Resp(Header header, Body body){}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Header(String resultCode, String resultMsg){}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Body(Items items, Integer numOfRows, Integer pageNo, Integer totalCount){}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Items(List<Item> item){}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Item(
            String contentid,
            String contenttypeid,
            String title,
            String addr1,
            String addr2,
            String areacode,
            String sigungucode,
            String cat1,
            String cat2,
            String cat3,
            String firstimage,
            String firstimage2,
            String zipcode,
            String tel,
            @JsonProperty("mapx") Double mapX, // 경도
            @JsonProperty("mapy") Double mapY  // 위도
    ){}
}
