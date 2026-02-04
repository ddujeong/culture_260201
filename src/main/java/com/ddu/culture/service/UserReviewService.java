package com.ddu.culture.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ddu.culture.dto.UserReviewRequest;
import com.ddu.culture.dto.UserReviewResponse;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserReview;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.repository.UserRepository;
import com.ddu.culture.repository.UserReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserReviewService {

    private final UserReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    // 1. 특정 아이템 리뷰 조회
    public List<UserReviewResponse> getReviewsByItem(Long itemId) {
        List<UserReview> reviews = reviewRepository.findByItemIdOrderByCreatedAtDesc(itemId);
        return reviews.stream()
                      .map(UserReviewResponse::from)
                      .collect(Collectors.toList());
    }

    // 2. 리뷰 작성
    public UserReviewResponse addReview(UserReviewRequest request) {
        User user = userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new RuntimeException("User not found"));
        Item item = itemRepository.findById(request.getItemId())
                        .orElseThrow(() -> new RuntimeException("Item not found"));

        UserReview review = new UserReview();
        review.setUser(user);
        review.setItem(item);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreatedAt(LocalDateTime.now());

        UserReview saved = reviewRepository.save(review);
        return UserReviewResponse.from(saved);
    }

}
