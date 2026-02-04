package com.ddu.culture.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

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
}
