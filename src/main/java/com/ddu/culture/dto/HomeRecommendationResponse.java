package com.ddu.culture.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HomeRecommendationResponse {
    private RecommendationResponse movie;
    private RecommendationResponse book;
    private RecommendationResponse music;
}
