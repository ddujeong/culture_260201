package com.ddu.culture.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Inheritance(strategy = InheritanceType.JOINED) // 자식 테이블과 조인하는 전략
@DiscriminatorColumn(name = "item_type")        // 구분을 위한 컬럼
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class) // 👈 이 줄을 추가하세요!
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;       

    @Enumerated(EnumType.STRING)
    private Category category;
    
    private String genre;
    @Column(columnDefinition = "TEXT")
    private String description; 
    private LocalDate releaseDate;

    private Double externalRating = 0.0; // TMDB, 알라딘 등 외부 평점 (보통 10점 만점 데이터가 많음)

    private Double averageRating = 0.0;  // 우리 서비스 유저 평균 평점 (5점 만점)

    private Integer reviewCount = 0;     // 평균 계산을 위한 리뷰 개수
    
    @Column(length = 2000)
    private String embeddingVector;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt; // 👈 새로 추가
    
    @Column(length = 2000)
    private String img;
    
    @ElementCollection
    @Column(name="tags")
    private List<String> tagsList;
    
    // 연관관계
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAction> actions = new ArrayList<>();
    
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserReview> reviews = new ArrayList<>();

    public void addReviewRating(int newRating) {
        double totalScore = (this.averageRating * this.reviewCount) + newRating;
        this.reviewCount++;
        this.averageRating = totalScore / this.reviewCount;
    }
}

