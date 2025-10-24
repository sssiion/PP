package com.example.pp.rec.model;

import reactor.core.publisher.Mono;

import java.util.List;

// Naver API 호출을 전담하는 서비스 인터페이스
public interface NaverApiService {

    /**
     * Naver 지역 검색 API (local.json)를 호출하여
     * AI가 파싱할 수 있는 원본(Raw) JSON 문자열을 반환합니다.
     * (txt: phase2_parse_map_data.txt 의 재료)
     * @param query (예: "강남역 카페")
     * @return Naver API가 반환한 Raw JSON 문자열
     */
    Mono<String> searchLocal(String query);

    /**
     * Naver 블로그 검색 API (blog.json)를 호출하여
     * 내용의 일부(snippet) 목록을 반환합니다.
     * (txt: summarize_external_data.txt 의 재료)
     * @param query (예: "스타벅스 강남점 후기")
     * @return 블로그 description 스니펫(요약글) 목록
     */
    Mono<List<String>> searchBlogSnippets(String query);
}