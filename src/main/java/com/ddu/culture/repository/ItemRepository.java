package com.ddu.culture.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

	List<Item> findByCategory(Category category);
	
	List<Item> findByCategoryAndGenre(Category category, String genre);
	
	List<Item> findByCategoryAndGenreIn(Category category, List<String> genres);
	
	List<Item> findTop10ByCategoryOrderByCreatedAtDesc(Category category);

}
