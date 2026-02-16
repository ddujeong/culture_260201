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
	
	public RecommendationResponse recommendForuserByCategory(Long userId, Category category, List<String> targetGenres) {
        // 1. 비회원 처리
        if (userId == null) {
            List<Item> topItems = (targetGenres == null || targetGenres.isEmpty()) 
                    ? itemRepository.findTop10ByCategoryOrderByCreatedAtDesc(category)
                    : itemRepository.findByCategoryAndGenreIn(category, targetGenres); // 리포지토리에 추가 필요

            List<RecommendationDto> dtos = topItems.stream()
                    .limit(8)
                    .map(item -> RecommendationDto.from(item, 5.0, RecommendationReason.POPULAR))
                    .collect(Collectors.toList());
            Collections.shuffle(dtos);
            return new RecommendationResponse(dtos);
        }

        // 2. 유저 선호/비선호 정보 조회
        List<UserPreferences> prefs = userPreferencesRepository.findByUserId(userId);
        List<String> preferredGenres = prefs.stream().map(UserPreferences::getGenre).distinct().toList();
        List<String> preferredTags = prefs.stream().map(UserPreferences::getTag).filter(Objects::nonNull).distinct().toList();

        // 비선호 장르 및 태그 합치기
        List<String> dislikedGenresTemp = prefs.stream().filter(p -> p.getWeight() < 0 && p.getGenre() != null).map(UserPreferences::getGenre).distinct().toList();
        List<String> reviewDislikedGenres = userReviewRepository.findByUserId(userId).stream().filter(r -> r.getRating() <= 2 && r.getItem() != null).map(r -> r.getItem().getGenre()).distinct().toList();
        final List<String> dislikedGenres = Stream.concat(dislikedGenresTemp.stream(), reviewDislikedGenres.stream()).distinct().toList();
        final List<String> dislikedTags = prefs.stream().filter(p -> p.getWeight() < 0 && p.getTag() != null).map(UserPreferences::getTag).distinct().toList();

        // 최근 행동 기반
        List<UserAction> recentActions = userActionRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Long> actionCounts = recentActions.stream().collect(Collectors.groupingBy(action -> action.getItem().getGenre(), Collectors.counting()));

        // 이미 본 아이템 제외용
        Set<Long> viewedItemIds = userActionRepository.findByUserId(userId).stream().map(action -> action.getItem().getId()).collect(Collectors.toSet());

        // 3. 후보 아이템 조회 (장르 필터링 포함)
        List<? extends Item> candidateItems;
        
        if (targetGenres != null && !targetGenres.isEmpty()) {
            // ✅ 카테고리(MOVIE)와 장르 리스트를 모두 만족하는 아이템만 조회해야 함
            candidateItems = itemRepository.findByCategoryAndGenreIn(category, targetGenres);
        } else {
            // targetGenres가 없으면 유저 선호 장르 + 최근 활동 장르 위주로 조회
        	List<String> fetchGenres = new ArrayList<>(Stream.concat(preferredGenres.stream(), actionCounts.keySet().stream()).collect(Collectors.toSet()));
            candidateItems = itemRepository.findByCategoryAndGenreIn(category, fetchGenres);
        }
        System.out.println("조회된 후보군 수: " + candidateItems.size());
        candidateItems = candidateItems.stream()
                .filter(item -> !viewedItemIds.contains(item.getId()))
                .distinct()
                .toList();
        // 후보가 너무 적으면 해당 카테고리 최신작으로 보충
        if (candidateItems.size() < 5) {
            candidateItems = itemRepository.findTop10ByCategoryOrderByCreatedAtDesc(category);
        }

        // 4. 점수 계산 및 정렬
        Map<String, Integer> genreCount = new HashMap<>();
        List<RecommendationDto> scoredItems = candidateItems.stream()
                .map(item -> {
                    double score = calculateScore(item, preferredGenres, actionCounts, preferredTags, dislikedGenres, dislikedTags);
                    
                    // 장르 반복 감점 (다양성 확보)
                    int count = genreCount.getOrDefault(item.getGenre(), 0);
                    score = Math.max(0.0, score - (count * 0.5));
                    genreCount.put(item.getGenre(), count + 1);

                    return RecommendationDto.from(item, score, generateReasonType(item, preferredGenres, actionCounts));
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(8)
                .toList();

        return new RecommendationResponse(scoredItems);
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

	public HomeRecommendationResponse recommendForHome(Long userId) {
	    // 1. 영화 섹션: 카테고리가 MOVIE인 것들 중, 장르가 '드라마'인 것은 제외합니다.
	    // (드라마 장르 영화는 너무 잔잔해서 '영화' 섹션의 성격과 안 맞을 수 있으니까요)
	    List<String> movieGenres = List.of("액션/어드벤처", "미스터리", "공포", "판타지/SF", "기타", "로맨스");
	    
	    // 2. 드라마 섹션: 카테고리가 'DRAMA'인 것 (ID 99, 100번대 데이터들)
	    // 3. 애니메이션 섹션: 카테고리가 'ANIMATION'인 것 (ID 5, 23번 데이터들)

	    return new HomeRecommendationResponse(
	        recommendForuserByCategory(userId, Category.MOVIE, movieGenres),      // 진짜 영화들
	        recommendForuserByCategory(userId, Category.DRAMA, null),              // TV 시리즈 드라마
	        recommendForuserByCategory(userId, Category.TV_SHOW, null),             // 예능 (데이터 확인 필요)
	        recommendForuserByCategory(userId, Category.ANIMATION, null),          // 애니메이션 전용
	        recommendForuserByCategory(userId, Category.BOOK, null),
	        recommendForuserByCategory(userId, Category.MUSIC, null)
	    );
	}
	
}
