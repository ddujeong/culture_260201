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
     * Type -> Category -> Genre 계층 구조 필터링
     */
    public List<ItemSummaryResponse> getItemsByFilter(String type, String category, String genre) {
        List<Item> items;

        // 1. 대분류 (Item Type: VIDEO, STATIC) 결정
        Class<? extends Item> itemClass = null;
        if ("VIDEO".equalsIgnoreCase(type)) {
            itemClass = VideoContent.class;
        } else if ("STATIC".equalsIgnoreCase(type)) {
            itemClass = StaticContent.class;
        }

        // 2. 중분류 (Category: MOVIE, DRAMA, ANIMATION 등) 결정
        Category cat = null;
        if (category != null && !"ALL".equalsIgnoreCase(category)) {
            try {
                cat = Category.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                cat = null;
            }
        }

        // 3. 필터 조합에 따른 Repository 호출 (장르는 ALL이 아닐 때만 필터링)
        boolean hasGenre = genre != null && !"ALL".equalsIgnoreCase(genre);

        if (itemClass != null && cat != null) {
            // [Type + Category] 가 확정된 상태
            items = itemRepository.findByItemTypeAndCategory(itemClass, cat);
        } else if (itemClass != null) {
            // [Type] 만 확정된 상태
            items = itemRepository.findByItemType(itemClass);
        } else if (cat != null) {
            // [Category] 만 확정된 상태 (Type 무관)
            items = itemRepository.findByCategory(cat);
        } else {
            // [전체보기]
            items = itemRepository.findAll();
        }

        // 4. 장르(Genre) 후처리 필터링 (DB에서 LIKE 쿼리로 가져와도 되지만, Stream으로 처리하면 더 유연함)
        if (hasGenre) {
            items = items.stream()
                    .filter(i -> i.getGenre() != null && i.getGenre().contains(genre))
                    .toList();
        }

        return items.stream()
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

