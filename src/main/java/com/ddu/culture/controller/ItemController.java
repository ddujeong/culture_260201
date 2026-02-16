package com.ddu.culture.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ddu.culture.dto.ItemDetailResponse;
import com.ddu.culture.dto.ItemSummaryResponse;
import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.service.ItemService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

	private final ItemService itemService;
	
	@GetMapping("/random")
	public ResponseEntity<?> getRandomItems(
			@RequestParam(name = "category") Category category,
			@RequestParam(defaultValue = "5", name = "limit") int limit) {
		List<Item> items = itemService.getRandomItemsByCategory(category, limit);
	    
	    // 엔티티 리스트를 DTO 리스트로 변환 (정석)
	    return ResponseEntity.ok(items.stream()
	            .map(ItemSummaryResponse::from)
	            .collect(Collectors.toList())) ;
	    }
	@GetMapping("/{id}")
    public ResponseEntity<ItemDetailResponse> getItem(@PathVariable(name = "id") Long id,
    		@RequestParam(required = false, name = "userId") Long userId) {
        ItemDetailResponse response = itemService.getItemDetail(id, userId);
        return ResponseEntity.ok(response);
    }
}
