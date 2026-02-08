package com.ddu.culture.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ddu.culture.dto.LoginRequest;
import com.ddu.culture.dto.LoginResponse;
import com.ddu.culture.dto.PreferencesRequest;
import com.ddu.culture.dto.PreferencesResponse;
import com.ddu.culture.dto.ReviewRequest;
import com.ddu.culture.dto.SignupRequest;
import com.ddu.culture.dto.UserReviewResponse;
import com.ddu.culture.dto.UserStatsResponse;
import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserPreferences;
import com.ddu.culture.entity.UserReview;
import com.ddu.culture.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;
	
	// 회원가입
	@PostMapping("/signup")
	public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
		User user = userService.signup(request);
		return ResponseEntity.ok(user);
	}
	
	// 취향 등록
	@PostMapping("/preferences")
	public ResponseEntity<?> registerPreferences(@RequestBody PreferencesRequest request) {
		List<PreferencesResponse> prefs = userService.registerPreferences(request);
		return ResponseEntity.ok(prefs);
	}
	
	// 임시 로그인 (삭제 예정)
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest request) {
	    User user = userService.login(request);
	    return ResponseEntity.ok(LoginResponse.from(user));
	}

	// 1️⃣ 사용자 통계/취향 조회
    @GetMapping("/{userId}/stats")
    public ResponseEntity<?> getUserStats(@PathVariable(name = "userId") Long userId) {
        UserStatsResponse stats = userService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }

    // 2️⃣ 사용자 리뷰 조회
    @GetMapping("/{userId}/reviews")
    public ResponseEntity<?> getUserReviewsByCategory(
            @PathVariable(name = "userId") Long userId,
            @RequestParam(required = false, name = "category") Category category) {
        List<UserReviewResponse> reviews;
        if (category != null) {
            reviews = userService.getUserReviewsByCategory(userId, category);
        } else {
            reviews = userService.getUserReviews(userId);
        }
        return ResponseEntity.ok(reviews);
    }


    // 3️⃣ 리뷰 수정
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable(name = "reviewId") Long reviewId,
            @RequestBody ReviewRequest request) {
        UserReviewResponse updated = userService.updateReview(reviewId, request);
        return ResponseEntity.ok(updated);
    }

    // 4️⃣ 리뷰 삭제
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable(name = "reviewId") Long reviewId) {
        userService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{userId}/avg-rating")
    public ResponseEntity<?> getAvgRatingByCategory(
            @PathVariable(name = "userId") Long userId,
            @RequestParam(name = "category") Category category) {
        return ResponseEntity.ok(userService.getAvgRatingByCategory(userId, category));
    }


}
