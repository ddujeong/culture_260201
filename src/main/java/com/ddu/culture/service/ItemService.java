package com.ddu.culture.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ddu.culture.dto.ItemDetailResponse;
import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.repository.ItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemService {

	private final ItemRepository itemRepository;
	
	public List<Item> getRandomItemsByCategory(Category category, int limit) {
		List<Item> items = itemRepository.findByCategory(category);
		Collections.shuffle(items);
		return items.stream().limit(limit).toList();
	}
	
	public ItemDetailResponse getItemDetail(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
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
        return ItemDetailResponse.from(item, otts);
    }
}

