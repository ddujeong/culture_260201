package com.ddu.culture.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserStatsResponse {
    private Double avgRating;
    private Long totalReviews;
    private String favoriteCategory;
    private List<String> dislikeGenres;
    private List<String> dislikeTags;
}



