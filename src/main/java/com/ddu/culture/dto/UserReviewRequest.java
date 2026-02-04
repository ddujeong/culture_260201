package com.ddu.culture.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserReviewRequest {
    private Long userId;
    private Long itemId;
    private int rating;
    private String comment;
}
