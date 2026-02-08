package com.ddu.culture.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecommendationReason {

    PREFERRED_GENRE("선호 장르 기반 추천이에요."),
    RECENT_ACTIVITY("최근 활동을 반영한 추천이에요."),
    HIGH_RATING("평점이 높은 콘텐츠예요."),
    POPULAR("많이 본 인기 콘텐츠예요.");

    private final String description;
}
