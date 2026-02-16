package com.ddu.culture.dto;

import java.util.stream.Collectors;

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

        if (item instanceof VideoContent vc) {
            // 🌟 엔티티의 Director 리스트에서 이름만 뽑아 쉼표로 합치기
            if (vc.getDirectors() != null) {
                dto.director = vc.getDirectors().stream()
                        .map(d -> d.getName())
                        .collect(Collectors.joining(", "));
            }
            
            // 🌟 엔티티의 Actor 리스트에서 이름만 뽑아 쉼표로 합치기 (최대 3명 정도가 적당)
            if (vc.getActors() != null) {
                dto.cast = vc.getActors().stream()
                        .map(a -> a.getName())
                        .limit(3) // 추천 리스트는 공간이 좁으니 3명까지만!
                        .collect(Collectors.joining(", "));
            }
            
        } else if (item instanceof StaticContent sc) {
            dto.creator = sc.getCreator();
        }

        return dto;
    }
}
