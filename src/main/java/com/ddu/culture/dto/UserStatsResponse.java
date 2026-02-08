package com.ddu.culture.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserStatsResponse {
    private Double avgRating;                // 평균 평점
    private Long totalReviews;               // 총 리뷰 수
    private String favoriteCategory;         // 선호 카테고리
    private List<String> dislikeGenres;      // 제외 장르
    private List<String> dislikeTags;        // 제외 태그
 // 새 필드: 장르/카테고리별 평균 평점
    private Map<String, Double> avgRatingByCategory; 
    private Map<String, Double> avgRatingByGenre;
}
