package com.ddu.culture.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ddu.culture.dto.RecommendationDto;
import com.ddu.culture.dto.RecommendationResponse;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.UserAction;
import com.ddu.culture.entity.UserPreferences;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.repository.UserActionRepository;
import com.ddu.culture.repository.UserPreferencesRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationService {

	private final ItemRepository itemRepository;
	private final UserPreferencesRepository userPreferencesRepository;
	private final UserActionRepository userActionRepository;
	
	public RecommendationResponse recommendForuserByCategory(Long userId, String category) {
		
		// 유저 선호 장르 조회
		List<UserPreferences> prefs = userPreferencesRepository.findByUserId(userId);
		List<String> preferredGenres = prefs.stream()
				.map(UserPreferences::getGenre)
				.distinct()
				.toList();
		
		// 최근 행동 기반 장르 가중치 계산
		List<UserAction> recentActions = userActionRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
		Map<String, Long> actionCounts = recentActions.stream()
				.collect(Collectors.groupingBy(
						action -> action.getItem().getGenre(),
						Collectors.counting()));
		
		// 후보 Item 조회
		Set<String> allGenres = new HashSet<>(preferredGenres);
		allGenres.addAll(actionCounts.keySet());

		
		List<Item> candidateItems = itemRepository.findByCategoryAndGenreIn(category, new ArrayList<>(allGenres))
				.stream()
				.distinct()
				.toList();
		
		// 점수계산 & DTO 변환
		List<RecommendationDto> recommendedItems = candidateItems.stream()
				.map(item -> {
					double score = calculateScore(item, preferredGenres, actionCounts);
					String reason = generateReason(item, preferredGenres, actionCounts);
					return new RecommendationDto(item.getId(),
							item.getTitle(),
							item.getCategory().name(),
							item.getGenre(), score, reason);
				}).sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
				.limit(10).toList();
		return new RecommendationResponse(recommendedItems);
	}
	
	// 점수 계산 로직
	private double calculateScore(Item item, List<String> preferredGenres, Map<String, Long> actionCounts) {
		
		double score = 0.0;
		
		// 선호 장르 가산
		if (preferredGenres.contains(item.getGenre())) score += 5.0;
		// 최근 행동 장르 가산
		score += actionCounts.getOrDefault(item.getGenre(), 0L) * 1.5;
		// 평점 계산
		double averageRating = item.getReviews().stream()
				.mapToDouble(r -> r.getRating())
				.average()
				.orElse(0.0);
		score += averageRating * 1.5;
		// 조회수 계산
		long viewCount = item.getActions().stream().count();
		score += viewCount * 0.01;
		
		return score;
	
	}
	
	// 추천 이유 생성
	private String generateReason(Item item, List<String> preferredGenres, Map<String, Long> actionCounts) {
		if (preferredGenres.contains(item.getGenre())) {
			return "선호하는 장르 기반 추천이에요.";
		} else if (actionCounts.containsKey(item.getGenre())) {
			return "최근 활동 장르 기반 추천이에요.";
		} else {
			return "인기 콘텐츠 추천이에요.";
		}
	}
	
	// 홈화면용 카테고별 추천
	public Map<String, RecommendationResponse> recommendForHome(Long userId) {
	    return Map.of(
	        "MOVIE", recommendForuserByCategory(userId, "MOVIE"),
	        "BOOK", recommendForuserByCategory(userId, "BOOK"),
	        "MUSIC", recommendForuserByCategory(userId, "MUSIC")
	    );
	}

}
