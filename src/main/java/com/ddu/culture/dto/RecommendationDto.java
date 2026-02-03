package com.ddu.culture.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendationDto {

    private Long itemId;
    private String title;
    private String type;
    private String genre;
    private Double score;
    private String reason;
}
