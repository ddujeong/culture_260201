package com.ddu.culture.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ddu.culture.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

	List<Item> findByCategory(String category);
	
	List<Item> findByCategoryAndGenre(String category, String genre);
}
