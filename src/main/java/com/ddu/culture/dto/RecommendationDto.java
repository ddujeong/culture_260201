package com.ddu.culture.dto;

import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.RecommendationReason;
import com.ddu.culture.entity.StaticContent;
import com.ddu.culture.entity.VideoContent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 장르 추천 DTO
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RecommendationDto {

    private Long itemId;
    private String title;
    private String category;
    private String genre;
    private double score;
    private RecommendationReason reasonType;
    private String reasonMessage;
    private String img;
    
    private String director; // 영상
    private String cast;     // 영상
    private String creator;  // 음악    
    public static RecommendationDto from(Item item, double score, RecommendationReason reasonType) {
        RecommendationDto dto = new RecommendationDto();
        dto.itemId = item.getId();
        dto.title = item.getTitle();
        dto.category = item.getCategory().name();
        dto.genre = item.getGenre();
        dto.score = score;
        dto.reasonType = reasonType;
        dto.reasonMessage = reasonType.getDescription();
        dto.img = item.getImg();

        // ⭐ 음악(StaticContent)인 경우 추가 필드 매핑
        if (item instanceof VideoContent vc) {
            dto.director = vc.getDirector();
            dto.cast = vc.getCast();
        } else if (item instanceof StaticContent sc) {
            dto.creator = sc.getCreator();
        }

        return dto;
    }
}
