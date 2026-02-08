package com.ddu.culture.dto;

import com.ddu.culture.entity.RecommendationReason;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendationReasonDto {
    private RecommendationReason type;
    private String message;
}