package com.ddu.culture.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class UserReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // 유저 한 명이 여러 리뷰
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)  // 영화 한 편에 여러 리뷰 가능
    @JoinColumn(name = "item_id")
    private Item item;

    private int rating; // 1~5 점

    @Column(length = 500)
    private String comment; // 한줄 코멘트

    private LocalDateTime timestamp;
}

