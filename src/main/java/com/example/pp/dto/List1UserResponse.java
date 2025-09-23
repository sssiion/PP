package com.example.pp.dto;

// 사용자에게 내려줄 최종 응답
public record List1UserResponse(
        java.time.LocalTime windowStart,   // 요청 시각
        java.time.LocalTime windowEnd,     // 요청 시각 +10분
        String dayType,                    // "DAY"
        java.util.List<LinePick> lines,    // 선택된 각 호선의 다음/다다음역 정보
        java.util.List<com.example.pp.dto.TourPoiResponse.Item> attractions // 관광지 결과(원문)
){

    public record LinePick(
            String lineParam,     // 예: "01"
            String humanLine,     // 예: "1호선"
            String baseStation,   // 기준역(예: "서울역")
            NextStation next1,    // 다음 역
            NextStation next2     // 다다음 역
    ){}

    public record NextStation(
            String name,          // 역명(접미사 "역" 포함)
            Double lon,           // 경도
            Double lat            // 위도
    ){}

}

