package com.ddu.culture.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ddu.culture.dto.LoginRequest;
import com.ddu.culture.dto.PreferencesRequest;
import com.ddu.culture.dto.PreferencesResponse;
import com.ddu.culture.dto.ReviewRequest;
import com.ddu.culture.dto.SignupRequest;
import com.ddu.culture.dto.UserReviewResponse;
import com.ddu.culture.dto.UserStatsResponse;
import com.ddu.culture.entity.ActionType;
import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserPreferences;
import com.ddu.culture.entity.UserReview;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.repository.UserActionRepository;
import com.ddu.culture.repository.UserPreferencesRepository;
import com.ddu.culture.repository.UserRepository;
import com.ddu.culture.repository.UserReviewRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional 
public class UserService {

	private final UserRepository userRepository;
	private final UserPreferencesRepository userPreferencesRepository;
	private final ItemRepository itemRepository;
    private final UserReviewRepository userReviewRepository; // 추가
   private final UserActionRepository userActionRepository;

	
	// 회원가입
	public User signup(SignupRequest request) {
		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setPassword(request.getPassword());
		return userRepository.save(user);
	}
	
	// 취향등록
	public List<PreferencesResponse> registerPreferences(PreferencesRequest request) {
		User user = userRepository.findById(request.getUserId())
				.orElseThrow(() -> new RuntimeException("user not found"));
		
		List<Item> selectedItems = itemRepository.findAllById(request.getItemsIds());
		
		List<UserPreferences> prefs = selectedItems.stream()
				.map(item -> {
					UserPreferences up = new UserPreferences();
					up.setUser(user);
					up.setItem(item);
					up.setCategory(item.getCategory());
					up.setGenre(item.getGenre());
					up.setWeight(1);
					return up;
				}).toList();
		List<UserPreferences> saved = userPreferencesRepository.saveAll(prefs);
		
		return saved.stream()
				.map(PreferencesResponse::from)
				.toList();
	}
	
	// 임시로그인
	public User login(LoginRequest request) {
	    User user = userRepository.findByEmail(request.getEmail())
	        .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자"));

	    if (!user.getPassword().equals(request.getPassword())) {
	        throw new RuntimeException("비밀번호 불일치");
	    }

	    return user;
	}

	// 1. 사용자 통계/취향
    public UserStatsResponse getUserStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found"));

        // 평균 평점
        Double avgRating = userReviewRepository.findAvgRatingByUserId(userId);
        if (avgRating == null) avgRating = 0.0;

        // 총 리뷰 수
        Long totalReviews = userReviewRepository.countByUserId(userId);
        // 선호 카테고리 (가장 많이 등록한 카테고리)
        List<String> favoriteCategories = userPreferencesRepository.findFavoriteCategory(userId);
        String favoriteCategory = favoriteCategories.isEmpty() ? "없음" : favoriteCategories.get(0);

        // 제외 장르/태그
        List<UserPreferences> prefs = userPreferencesRepository.findByUserId(userId);
        List<String> dislikeGenres = new ArrayList<>(
        	    prefs.stream()
        	         .filter(p -> p.getWeight() < 0 && p.getGenre() != null)
        	         .map(UserPreferences::getGenre)
        	         .distinct()
        	         .toList()
        	);
     // 🔹 리뷰 기반 비선호 장르 추가 (평점 2점 이하)
        List<UserReview> reviews = userReviewRepository.findByUserId(userId);
        List<String> dislikedFromReviews = reviews.stream()
                .filter(r -> r.getRating() <= 2)   // 1~2점 리뷰
                .filter(r -> r.getItem() != null && r.getItem().getGenre() != null)
                .map(r -> r.getItem().getGenre())
                .distinct()
                .toList();

        // 기존 dislikeGenres에 합치기
        dislikeGenres.addAll(dislikedFromReviews);
        dislikeGenres = dislikeGenres.stream().distinct().toList();  // 중복 제거
        
        List<String> dislikeTags = prefs.stream()
                .filter(p -> p.getWeight() < 0 && p.getTag() != null)
                .map(UserPreferences::getTag)
                .distinct()
                .toList();

     // 2️⃣ 장르/카테고리별 평균 평점 계산

        Map<String, Double> avgRatingByCategory = reviews.stream()
                .filter(r -> r.getItem() != null && r.getItem().getCategory() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getItem().getCategory().toString(), // String으로 변환
                        Collectors.averagingDouble(UserReview::getRating)
                ));

        Map<String, Double> avgRatingByGenre = reviews.stream()
                .filter(r -> r.getItem() != null && r.getItem().getGenre() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getItem().getGenre().toString(),
                        Collectors.averagingDouble(UserReview::getRating)
                ));


        return new UserStatsResponse(
                avgRating,
                totalReviews,
                favoriteCategory,
                dislikeGenres,
                dislikeTags,
                avgRatingByCategory,
                avgRatingByGenre
        );   }

    // 2. 리뷰/평점 조회
    public List<UserReviewResponse> getUserReviews(Long userId) {
    	
    	return userReviewRepository.findByUserId(userId).stream()
                .map(UserReviewResponse::from)
                .toList();    }
    public List<UserReviewResponse> getUserReviewsByCategory(Long userId, Category category) {
        return userReviewRepository.findByUserIdAndCategory(userId, category).stream()
                .map(UserReviewResponse::from)
                .toList();
    }

    // 3. 리뷰 수정
    public UserReviewResponse updateReview(Long reviewId, ReviewRequest request) {
        UserReview review = userReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("review not found"));

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return UserReviewResponse.from(review);
    }

    // 4. 리뷰 삭제
    public void deleteReview(Long reviewId) {
        userReviewRepository.deleteById(reviewId);
    }
    
    public Map<String, Long> getGenreStatsByAction(Long userId) {
        List<Object[]> results = userActionRepository.countGenresByUser(userId, ActionType.WATCHED);

        Map<String, Long> genreStats = new HashMap<>();
        for (Object[] row : results) {
            genreStats.put((String) row[0], (Long) row[1]);
        }
        return genreStats;
    }
    public Map<Integer, Long> getRatingDistribution(Long userId) {
        List<Object[]> results = userReviewRepository.countRatingByUserId(userId);

        Map<Integer, Long> ratingStats = new HashMap<>();
        for (Object[] row : results) {
            ratingStats.put((Integer) row[0], (Long) row[1]);
        }
        return ratingStats;
    }
    public Map<String, Double> getAvgRatingByCategory(Long userId, Category category) {
        List<UserReview> reviews = userReviewRepository.findByUserId(userId);

        return reviews.stream()
                .filter(r -> r.getItem() != null && category.equals(r.getItem().getCategory()))
                .filter(r -> r.getItem().getGenre() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getItem().getGenre(),
                        Collectors.averagingDouble(UserReview::getRating)
                ));
    }


}
