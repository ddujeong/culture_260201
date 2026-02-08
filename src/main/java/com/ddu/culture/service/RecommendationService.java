package com.ddu.culture.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.ddu.culture.dto.HomeRecommendationResponse;
import com.ddu.culture.dto.RecommendationDto;
import com.ddu.culture.dto.RecommendationResponse;
import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.RecommendationReason;
import com.ddu.culture.entity.UserAction;
import com.ddu.culture.entity.UserPreferences;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.repository.UserActionRepository;
import com.ddu.culture.repository.UserPreferencesRepository;
import com.ddu.culture.repository.UserReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationService {

	private final ItemRepository itemRepository;
	private final UserPreferencesRepository userPreferencesRepository;
	private final UserActionRepository userActionRepository;
	private final UserReviewRepository userReviewRepository;
	
	public RecommendationResponse recommendForuserByCategory(Long userId, Category category) {
		
		// 유저 선호 장르 조회
		List<UserPreferences> prefs = userPreferencesRepository.findByUserId(userId);
		List<String> preferredGenres = prefs.stream()
				.map(UserPreferences::getGenre)
				.distinct()
				.toList();
		List<String> preferredTags = prefs.stream()
                .map(UserPreferences::getTag)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
		// 비선호 장르 (weight < 0)
		List<String> dislikedGenresTemp = prefs.stream()
		        .filter(p -> p.getWeight() < 0 && p.getGenre() != null)
		        .map(UserPreferences::getGenre)
		        .distinct()
		        .toList();

		// 리뷰 기반 비선호 장르 (1~2점)
		List<String> reviewDislikedGenres = userReviewRepository.findByUserId(userId).stream()
		        .filter(r -> r.getRating() <= 2)
		        .filter(r -> r.getItem() != null && r.getItem().getGenre() != null)
		        .map(r -> r.getItem().getGenre())
		        .distinct()
		        .toList();

		// 합쳐서 final 선언
		final List<String> dislikedGenres = Stream.concat(dislikedGenresTemp.stream(), reviewDislikedGenres.stream())
		        .distinct()
		        .toList();

		// 비선호 태그
		final List<String> dislikedTags = prefs.stream()
		        .filter(p -> p.getWeight() < 0 && p.getTag() != null)
		        .map(UserPreferences::getTag)
		        .distinct()
		        .toList();
   
		// 최근 행동 기반 장르 가중치 계산
		List<UserAction> recentActions = userActionRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
		Map<String, Long> actionCounts = recentActions.stream()
				.collect(Collectors.groupingBy(
						action -> action.getItem().getGenre(),
						Collectors.counting()));
		
		// 후보 Item 조회
		List<String> allGenres = new ArrayList<>(
			    Stream.concat(preferredGenres.stream(), actionCounts.keySet().stream())
			          .collect(Collectors.toSet()) // 중복 제거
			);
			Collections.shuffle(allGenres); // 이제 안전

		
		Set<Long> viewedItemIds = userActionRepository
		        .findByUserId(userId)
		        .stream()
		        .map(action -> action.getItem().getId())
		        .collect(Collectors.toSet());

		
		List<Item> candidateItems = itemRepository
		        .findByCategoryAndGenreIn(category, new ArrayList<>(allGenres))
		        .stream()
		        .filter(item -> !viewedItemIds.contains(item.getId()))
		        .distinct()
		        .toList();

		if(candidateItems.isEmpty()) {
		    candidateItems = itemRepository.findTop10ByCategoryOrderByCreatedAtDesc(category);
		}

		 // 4️⃣ 점수 계산 & 장르 반복 감점
	    Map<String, Integer> genreCount = new HashMap<>();
	    List<RecommendationDto> scoredItems = candidateItems.stream()
	            .map(item -> {
	                double score = calculateScore(item, preferredGenres, actionCounts, preferredTags, dislikedGenres, dislikedTags);

	                // 장르 반복 감점
	                int count = genreCount.getOrDefault(item.getGenre(), 0);
	                score -= count * 0.5; // 같은 장르가 많으면 감점
	                genreCount.put(item.getGenre(), count + 1);

	                RecommendationReason reasonType = generateReasonType(item, preferredGenres, actionCounts);
	                return RecommendationDto.from(item, score, reasonType);
	            })
	            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
	            .limit(8) // 상위 8개
	            .toList();

	    // 5️⃣ 랜덤/인기 아이템 2개 섞기
	    List<Item> fallbackItems = new ArrayList<>(
	    	    itemRepository.findTop10ByCategoryOrderByCreatedAtDesc(category)
	    	                  .stream()
	    	                  .filter(item -> !viewedItemIds.contains(item.getId()))
	    	                  .filter(item -> scoredItems.stream().noneMatch(dto -> dto.getItemId().equals(item.getId())))
	    	                  .distinct()
	    	                  .toList() // ← 이건 Immutable
	    	);
	    	Collections.shuffle(fallbackItems);

	 // fallbackItems를 RecommendationDto로 변환
	    List<RecommendationDto> fallbackDtos = fallbackItems.stream()
	            .map(item -> RecommendationDto.from(
	                    item,
	                    calculateScore(item, preferredGenres, actionCounts, preferredTags, dislikedGenres, dislikedTags),
	                    RecommendationReason.POPULAR
	            ))
	            .limit(2)
	            .toList();

	    // scoredItems + fallbackDtos 합치기
	    List<RecommendationDto> finalItems = Stream.concat(scoredItems.stream(), fallbackDtos.stream())
	            .toList();

	    return new RecommendationResponse(finalItems);
	}
	
	// 점수 계산
    private double calculateScore(Item item, List<String> preferredGenres, Map<String, Long> actionCounts,
                                  List<String> userPreferredTags, List<String> userDislikedGenres, List<String> userDislikedTags) {

        double score = 0.0;

        // 선호 장르
        if (preferredGenres.contains(item.getGenre())) score += 2.0;
        // 최근 행동 장르
        score += actionCounts.getOrDefault(item.getGenre(), 0L) * 2.0;
        // 평점
        Double avg = userReviewRepository.findAvgRatingByItemId(item.getId());
        double averageRating = avg != null ? avg : 0.0;
        score += averageRating * 2.0;
        // 조회수
        long viewCount = userActionRepository.countByItemId(item.getId());
        score += viewCount * 0.05;
        // 사용자 태그
        for (String tag : item.getTagsList()) {
            if (userPreferredTags.contains(tag)) score += 1.0;
            if (userDislikedTags.contains(tag)) score -= 1.0;
        }
        // 비선호 장르 감점
        if (userDislikedGenres.contains(item.getGenre())) score -= 0.2; // 점수만 낮춤, 후보 유지
        
     // 점수 계산 후
        score = Math.max(0.0, Math.min(score, 5.0)); // 0~5로 제한


        return score;
    }
	
	// 추천 이유 생성
	public RecommendationReason generateReasonType(
	        Item item,
	        List<String> preferredGenres,
	        Map<String, Long> actionCounts
	) {
	    Double avg = userReviewRepository.findAvgRatingByItemId(item.getId());
	    double averageRating = avg != null ? avg : 0.0;

	    if (averageRating >= 4.5) {
	        return RecommendationReason.HIGH_RATING;
	    }

	    if (actionCounts.getOrDefault(item.getGenre(), 0L) >= 3) {
	        return RecommendationReason.RECENT_ACTIVITY;
	    }

	    if (preferredGenres.contains(item.getGenre())) {
	        return RecommendationReason.PREFERRED_GENRE;
	    }

	    return RecommendationReason.POPULAR;
	}

	public RecommendationReason generateReasonType(Long userId, Item item) {

	    List<String> preferredGenres =
	        userPreferencesRepository.findByUserId(userId)
	            .stream()
	            .map(UserPreferences::getGenre)
	            .distinct()
	            .toList();

	    Map<String, Long> actionCounts =
	        userActionRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
	            .stream()
	            .collect(Collectors.groupingBy(
	                action -> action.getItem().getGenre(),
	                Collectors.counting()
	            ));

	    return generateReasonType(item, preferredGenres, actionCounts);
	}
	public String buildDetailMessage(
	        RecommendationReason reason,
	        Item item
	) {
	    return switch (reason) {
	        case PREFERRED_GENRE ->
	            "평소에 좋아하시는 '" + item.getGenre() + "' 장르 콘텐츠라서 추천했어요.";

	        case RECENT_ACTIVITY ->
	            "최근 '" + item.getGenre() + "' 장르 콘텐츠를 자주 보셔서 추천했어요.";

	        case HIGH_RATING ->
	            "평점이 높아 많은 사용자에게 좋은 평가를 받고 있어요.";

	        case POPULAR ->
	            "최근 많이 소비되고 있는 인기 콘텐츠예요.";
	    };
	}

	
	// 홈화면용 카테고별 추천
	public HomeRecommendationResponse recommendForHome(Long userId) {
	    return new HomeRecommendationResponse(
	            recommendForuserByCategory(userId, Category.MOVIE),
	            recommendForuserByCategory(userId, Category.BOOK),
	            recommendForuserByCategory(userId, Category.MUSIC)
	        );

	}
	
}
