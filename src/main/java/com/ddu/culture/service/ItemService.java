package com.ddu.culture.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ddu.culture.dto.ItemDetailResponse;
import com.ddu.culture.dto.ItemScoreDto;
import com.ddu.culture.dto.ItemSummaryResponse;
import com.ddu.culture.dto.RecommendationReasonDto;
import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.StaticContent;
import com.ddu.culture.entity.User;
import com.ddu.culture.entity.VideoContent;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.repository.UserPreferencesRepository;
import com.ddu.culture.repository.UserRepository;
import com.ddu.culture.repository.UserReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemService {

	private final ItemRepository itemRepository;
	private final UserReviewRepository userReviewRepository;
	private final RecommendationService recommendationService;
	private final UserRepository userRepository;
	private final UserPreferencesRepository userPreferencesRepository;

	
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
        List<ItemDetailResponse.OTTInfo> otts = new ArrayList<>();
        if (item instanceof VideoContent vc && vc.getOttProviders() != null) {
            String[] providers = vc.getOttProviders().split(", ");
            for (String providerName : providers) {
                otts.add(createOttInfo(providerName.trim(), item.getTitle()));
            }
        }
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
	// ... 상단 어노테이션 및 필드 생략 ...

	/**
	 * Type -> Category -> Genre 계층 구조 + 검색(Search) + 정렬(Sort) 필터링
	 */
	public List<ItemSummaryResponse> getItemsByFilter(String type, String category, String genre, String search, String sort) {
	    List<Item> items;

	    // 1. 대분류 (Item Type) 결정
	    Class<? extends Item> itemClass = null;
	    if ("VIDEO".equalsIgnoreCase(type)) itemClass = VideoContent.class;
	    else if ("STATIC".equalsIgnoreCase(type)) itemClass = StaticContent.class;

	    // 2. 중분류 (Category) 결정
	    Category cat = null;
	    if (category != null && !"ALL".equalsIgnoreCase(category)) {
	        try { cat = Category.valueOf(category.toUpperCase()); } catch (Exception e) {}
	    }

	    // 3. Repository 기초 조회 (기존 로직 유지)
	    if (itemClass != null && cat != null) {
	        items = itemRepository.findByItemTypeAndCategory(itemClass, cat);
	    } else if (itemClass != null) {
	        items = itemRepository.findByItemType(itemClass);
	    } else if (cat != null) {
	        items = itemRepository.findByCategory(cat);
	    } else {
	        items = itemRepository.findAll();
	    }

	    // 4. Stream을 이용한 검색, 장르 필터링 및 정렬 처리
	    return items.stream()
	            // [장르 필터링]
	            .filter(i -> genre == null || "ALL".equalsIgnoreCase(genre) || 
	                   (i.getGenre() != null && i.getGenre().contains(genre)))
	            // [검색어 필터링] 제목 기준
	            .filter(i -> search == null || search.isBlank() || 
	                   i.getTitle().toLowerCase().contains(search.toLowerCase()))
	            // [정렬 처리]
	            .sorted((a, b) -> {
	                if ("rating".equalsIgnoreCase(sort)) {
	                    // 별점 높은 순 (Double 비교)
	                    return Double.compare(b.getExternalRating(), a.getExternalRating());
	                } else if ("oldest".equalsIgnoreCase(sort)) {
	                    // 오래된 등록 순
	                    return a.getCreatedAt().compareTo(b.getCreatedAt());
	                }
	                // 기본: 최신 등록 순 (newest)
	                return b.getCreatedAt().compareTo(a.getCreatedAt());
	            })
	            .map(ItemSummaryResponse::from)
	            .toList();
	}
	private ItemDetailResponse.OTTInfo createOttInfo(String name, String title) {
		String encodedTitle = java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8);
		
		return switch (name) {
        case "Netflix" -> new ItemDetailResponse.OTTInfo(name, "https://www.netflix.com/search?q=" + encodedTitle, "#E50914", "https://upload.wikimedia.org/wikipedia/commons/0/08/Netflix_2015_logo.svg");
        case "Disney+" -> new ItemDetailResponse.OTTInfo(name, "https://www.disneyplus.com/" ,"#113CCF", "https://upload.wikimedia.org/wikipedia/commons/3/3e/Disney%2B_logo.svg");
        case "티빙" -> new ItemDetailResponse.OTTInfo(name, "https://www.tving.com/search?keyword=" + encodedTitle, "#FF153C", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7e/TVING.png/250px-TVING.png");
        case "wavve" -> new ItemDetailResponse.OTTInfo(name, "https://www.wavve.com/search/search?searchWord=" + encodedTitle, "#0051FF", "https://i.namu.wiki/i/TtTVXUomuPTQL4GTdJ-bKwsvuBCO_vu9_bQRmBJEvCu8ivQ_kje6PIB8HiGrjSYQ6KeGGv68o8u8YRpTpzUyWg.svg");
        case "왓챠" -> new ItemDetailResponse.OTTInfo(name, "https://watcha.com/search?query=" + encodedTitle, "#FF2D55", "https://upload.wikimedia.org/wikipedia/commons/b/b8/왓챠_로고_2021.png");
        case "쿠팡플레이" -> new ItemDetailResponse.OTTInfo(name, "https://www.coupangplay.com/search?q=" + encodedTitle, "#00A9FF", "https://upload.wikimedia.org/wikipedia/commons/d/d4/Coupang_Play_logo.png");
        case "Apple TV+" -> new ItemDetailResponse.OTTInfo(name, "https://tv.apple.com/kr/search?term=" + encodedTitle, "#000000", "https://upload.wikimedia.org/wikipedia/commons/2/28/Apple_TV_Plus_Logo.svg");
        case "Amazon Prime Video" -> new ItemDetailResponse.OTTInfo(name, "https://www.primevideo.com/search/ref=atv_nb_sr?phrase=" + encodedTitle, "#1A242E", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Amazon_Prime_Video_logo_%282022%29.svg/500px-Amazon_Prime_Video_logo_%282022%29.svg.png");
        default -> new ItemDetailResponse.OTTInfo(name, "#", "#666666", "");
    };
	}
}

