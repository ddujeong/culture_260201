package com.ddu.culture.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ddu.culture.dto.LoginRequest;
import com.ddu.culture.dto.PreferencesRequest;
import com.ddu.culture.dto.PreferencesResponse;
import com.ddu.culture.dto.ReviewRequest;
import com.ddu.culture.dto.SignupRequest;
import com.ddu.culture.dto.UserReviewResponse;
import com.ddu.culture.dto.UserStatsResponse;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserPreferences;
import com.ddu.culture.entity.UserReview;
import com.ddu.culture.repository.ItemRepository;
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
        List<String> dislikeGenres = prefs.stream()
                .filter(p -> p.getWeight() < 0 && p.getGenre() != null)
                .map(UserPreferences::getGenre)
                .distinct()
                .toList();

        List<String> dislikeTags = prefs.stream()
                .filter(p -> p.getWeight() < 0 && p.getTag() != null)
                .map(UserPreferences::getTag)
                .distinct()
                .toList();

        return new UserStatsResponse(avgRating, totalReviews, favoriteCategory, dislikeGenres, dislikeTags);
    }

    // 2. 리뷰/평점 조회
    public List<UserReviewResponse> getUserReviews(Long userId) {
    	
    	return userReviewRepository.findByUserId(userId).stream()
                .map(UserReviewResponse::from)
                .toList();    }

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
}
