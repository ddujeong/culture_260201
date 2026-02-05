package com.ddu.culture.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private int rating;
    private String comment;
}
