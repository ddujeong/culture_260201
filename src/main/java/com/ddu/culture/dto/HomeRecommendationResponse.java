package com.ddu.culture.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class HomeRecommendationResponse {
    private RecommendationResponse movie;         // 영화
    private RecommendationResponse drama;         // 드라마
    private RecommendationResponse entertainment;   // 예능
    private RecommendationResponse animation;     // 애니메이션
    private RecommendationResponse book;          // 책
    private RecommendationResponse music;         // 음악
}