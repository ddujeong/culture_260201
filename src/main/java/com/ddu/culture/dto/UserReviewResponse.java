package com.ddu.culture.dto;

import java.time.LocalDateTime;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.UserReview;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserReviewResponse {
    private Long id;
    private Long itemId;
    private Long userId;
    private String username; // 유저 이름
    private int rating;
    private String comment;
    private Category category; // ← 추가
    private LocalDateTime createdAt;
    private String title; // ← 추가


    public static UserReviewResponse from(UserReview review) {
        UserReviewResponse dto = new UserReviewResponse();
        dto.id = review.getId();
        dto.itemId = review.getItem().getId();
        dto.userId = review.getUser() != null ? review.getUser().getId() : null; // 추가
        dto.username = review.getUser().getUsername();
        dto.rating = review.getRating();
        dto.comment = review.getComment();
        dto.category = review.getItem() != null ? review.getItem().getCategory() : null; // ← 추가
        dto.createdAt = review.getCreatedAt();
        dto.title = review.getItem().getTitle(); // ← 여기서 아이템 제목 가져오기

        return dto;
    }
}
