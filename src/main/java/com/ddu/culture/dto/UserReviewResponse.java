package com.ddu.culture.dto;

import java.time.LocalDateTime;

import com.ddu.culture.entity.UserReview;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserReviewResponse {
    private Long id;
    private Long userId;
    private String username; // 유저 이름
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    public static UserReviewResponse from(UserReview review) {
        UserReviewResponse dto = new UserReviewResponse();
        dto.id = review.getId();
        dto.username = review.getUser().getUsername();
        dto.rating = review.getRating();
        dto.comment = review.getComment();
        dto.createdAt = review.getCreatedAt();
        return dto;
    }
}
