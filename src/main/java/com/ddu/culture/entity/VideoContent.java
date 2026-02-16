package com.ddu.culture.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("VIDEO")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class VideoContent extends Item {
    
    private String director;      // 감독 (TV는 Created By)
    
    @Column(length = 1000)
    private String cast;          // 주요 출연진
    
    private String ottProviders;  // 시청 가능 OTT (Netflix, Disney Plus 등)
    
    private Integer runtime;      // 영화: 러닝타임 / TV: 에피소드 평균 시간
    
    // TV 시리즈 전용 (드라마, 예능, TV애니메이션)
    private Integer totalSeasons; 
    private Integer totalEpisodes;
    
    private String originCountry; // 제작 국가
}
