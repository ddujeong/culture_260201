package com.ddu.culture.dto;

import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.RecommendationReason;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 장르 추천 DTO
@Getter
@AllArgsConstructor
public class RecommendationDto {

    private Long itemId;
    private String title;
    private String category;
    private String genre;
    private double score;
    private RecommendationReason reasonType;
    private String reasonMessage;
    
    public static RecommendationDto from(Item item, double score,RecommendationReason reasonType) {
        return new RecommendationDto(
            item.getId(),
            item.getTitle(),
            item.getCategory().name(),
            item.getGenre(),
            score,
            reasonType,
            reasonType.getDescription()
        );
    }
}
