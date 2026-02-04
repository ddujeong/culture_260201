package com.ddu.culture.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ddu.culture.dto.UserReviewRequest;
import com.ddu.culture.dto.UserReviewResponse;
import com.ddu.culture.service.UserReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class UserReviewController {

    private final UserReviewService reviewService;

    // 특정 아이템 리뷰 조회
    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<UserReviewResponse>> getReviews(@PathVariable(name = "itemId") Long itemId) {
        return ResponseEntity.ok(reviewService.getReviewsByItem(itemId));
    }

    // 리뷰 작성
    @PostMapping
    public ResponseEntity<UserReviewResponse> addReview(@RequestBody UserReviewRequest request) {
        return ResponseEntity.ok(reviewService.addReview(request));
    }

}
