package com.ddu.culture.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.RecommendationReason;
import com.ddu.culture.entity.UserReview;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemDetailResponse {
    private Long id;
    private String title;
    private String genre;
    private Category category;
    private String description; // 아이템 설명
    private String img; // 이미지 URL
    private List<OTTInfo> otts; // 시청 가능한 OTT
    private double averageRating;
    private LocalDate releaseDate;
    private RecommendationReasonDto recommendationReason; // ⭐ 추가
    
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OTTInfo {
        private String name;
        private String url;
        private String color;
        private String logoUrl;
    }
    
    public static ItemDetailResponse from(Item item, List<OTTInfo> otts, double averageRating) {
		ItemDetailResponse dto = new ItemDetailResponse();
		dto.id = item.getId();
		dto.title = item.getTitle();
		dto.genre = item.getGenre();
		dto.category = item.getCategory();
		dto.description = item.getDescription();
		dto.img = item.getImg();
		dto.otts = otts;
		dto.averageRating = averageRating;
		dto.releaseDate = item.getReleaseDate();
		dto.recommendationReason =
			    new RecommendationReasonDto(
			        RecommendationReason.PREFERRED_GENRE,
			        "선호 장르 기반 추천이에요."
			    );

		return dto;
		
	}
}
