package com.ddu.culture.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ddu.culture.dto.ItemDetailResponse;
import com.ddu.culture.dto.RecommendationReasonDto;
import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.repository.UserReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemService {

	private final ItemRepository itemRepository;
	private final UserReviewRepository userReviewRepository;
	private final RecommendationService recommendationService;

	
	public List<Item> getRandomItemsByCategory(Category category, int limit) {
		List<Item> items = itemRepository.findByCategory(category);
		Collections.shuffle(items);
		return items.stream().limit(limit).toList();
	}
	
	public ItemDetailResponse getItemDetail(Long itemId, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        Double avg = userReviewRepository.findAvgRatingByItemId(itemId);
        double averageRating = avg != null ? avg : 0.0;
        List<ItemDetailResponse.OTTInfo> otts = List.of(
                new ItemDetailResponse.OTTInfo(
                        "Netflix",
                        "https://www.netflix.com",
                        "#E50914",
                        "https://upload.wikimedia.org/wikipedia/commons/0/08/Netflix_2015_logo.svg"
                ),
                new ItemDetailResponse.OTTInfo(
                        "Watcha",
                        "https://watcha.com",
                        "#FF2D55",
                        "https://upload.wikimedia.org/wikipedia/commons/b/b8/왓챠_로고_2021.png"
                ),
                new ItemDetailResponse.OTTInfo(
                        "Disney+",
                        "https://www.disneyplus.com",
                        "#113CCF",
                        "https://upload.wikimedia.org/wikipedia/commons/3/3e/Disney%2B_logo.svg"
                )
        );
        ItemDetailResponse response =
                ItemDetailResponse.from(item, otts, averageRating);

        // ⭐ 여기서부터 추천 이유 추가
        if (userId != null) {
            var reasonType =
                    recommendationService.generateReasonType(userId, item);
            String message =
                    recommendationService.buildDetailMessage(reasonType, item);

            response.setRecommendationReason(
                    new RecommendationReasonDto(reasonType, message)
            );

        }

        return response;
    }
}

