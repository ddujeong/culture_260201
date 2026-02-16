package com.ddu.culture.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ddu.culture.entity.Category;
import com.ddu.culture.service.RecommendationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {

	private final RecommendationService recommendationService;
	
	// 홈화면 추천용 (노래, 영화, 책 모두)
	@GetMapping("/home")
	public ResponseEntity<?> getHomeRecommendations(@RequestParam(name = "userId", required = false) Long userId) {
		return ResponseEntity.ok(recommendationService.recommendForHome(userId));
	}
	
	// 특정 카테고리별 추천
	@GetMapping("/category")
	public ResponseEntity<?> getCategoryRecommendations(@RequestParam(name = "userId", required = false) Long userId, @RequestParam(name = "category") Category category) {
		return ResponseEntity.ok(recommendationService.recommendForuserByCategory(userId, category,null));
	}
}
