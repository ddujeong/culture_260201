package com.ddu.culture.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ddu.culture.entity.Category;
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
		return ResponseEntity.ok(itemService.getRandomItemsByCategory(category, limit));
	}
}
