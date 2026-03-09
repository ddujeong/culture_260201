package com.ddu.culture.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.RecommendationReason;
import com.ddu.culture.entity.StaticContent;
import com.ddu.culture.entity.UserReview;
import com.ddu.culture.entity.VideoContent;

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
    private double externalRating; // 👈 이게 있어야 프론트로 전달됩니다!
    private LocalDate releaseDate;
    private RecommendationReasonDto recommendationReason; // ⭐ 추가
    private List<PersonDto> actors;    
    private List<PersonDto> directors;
    private Integer runtime;
    private Integer totalSeasons;
    private Integer totalEpisodes;
    private String originCountry;
    private String itemType; // ⭐ 추가: "VIDEO" 또는 "STATIC" (DTYPE 역할)
    private List<SeasonDto> seasons;
    
    private String creator;
    private String publisher;
    
    @Getter @AllArgsConstructor
    public static class PersonDto {
        private String name;
        private String profilePath;
    }

    // 시즌 정보를 담을 내부 클래스
    @Getter @AllArgsConstructor
    public static class SeasonDto {
        private int seasonNumber;
        private String name;
        private String overview;
        private String posterPath;
        private int episodeCount;
        private String airDate;
    }
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
		dto.externalRating = item.getExternalRating();
		dto.img = item.getImg();
		dto.otts = otts;
		dto.averageRating = averageRating;
		dto.releaseDate = item.getReleaseDate();
		// ⭐ 음악 데이터(StaticContent)인 경우 필드 추가 매핑
		if (item instanceof StaticContent sc) { // Java 17+ 패턴 매칭 사용
			dto.itemType = "STATIC"; // 프론트와 약속된 타입명
			dto.creator = sc.getCreator();
			dto.publisher = sc.getPublisher();
        }else if (item instanceof VideoContent vc) {
        	dto.itemType = "VIDEO"; // 프론트와 약속된 타입명
        	// 🌟 배우 리스트 매핑 (Entity -> DTO)
            dto.actors = vc.getActors().stream()
                .map(a -> new PersonDto(a.getName(), a.getProfilePath()))
                .toList();

            // 🌟 감독 리스트 매핑
            dto.directors = vc.getDirectors().stream()
                .map(d -> new PersonDto(d.getName(), d.getProfilePath()))
                .toList();
            
            // 🌟 시즌 리스트 매핑
            dto.seasons = vc.getSeasons().stream()
                .map(s -> new SeasonDto(
                    s.getSeasonNumber(), s.getName(), s.getOverview(), 
                    s.getPosterPath(), s.getEpisodeCount(), s.getAirDate()
                ))
                .toList();
            dto.runtime = vc.getRuntime();
            dto.totalSeasons = vc.getTotalSeasons();
            dto.totalEpisodes = vc.getTotalEpisodes();
            dto.originCountry = vc.getOriginCountry();
        }
		dto.recommendationReason =
			    new RecommendationReasonDto(
			        RecommendationReason.PREFERRED_GENRE,
			        "선호 장르 기반 추천이에요."
			    );

		return dto;
		
	}
}
