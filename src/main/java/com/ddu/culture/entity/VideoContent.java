package com.ddu.culture.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("VIDEO")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class VideoContent extends Item {
	@Column(unique = true)
    private Long tmdbId;   // TMDB 고유 ID (중복 방지용)
	
    private String ottProviders;  // 시청 가능 OTT (Netflix, Disney Plus 등)
    
    private Integer runtime;      // 영화: 러닝타임 / TV: 에피소드 평균 시간
    
    // TV 시리즈 전용 (드라마, 예능, TV애니메이션)
    private Integer totalSeasons; 
    private Integer totalEpisodes;
    
    private String originCountry; // 제작 국가
    private Double popularity;
    private Integer voteCount;
 // VideoContent.java 내부에 추가
    @OneToMany(mappedBy = "videoContent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Season> seasons = new ArrayList<>();
    
    // 🌟 출연진 정보 (추가)
    @OneToMany(mappedBy = "videoContent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Actor> actors = new ArrayList<>();

    // 🌟 감독 정보 (추가)
    @OneToMany(mappedBy = "videoContent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Director> directors = new ArrayList<>();
}
