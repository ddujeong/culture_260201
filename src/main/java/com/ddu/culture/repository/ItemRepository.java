package com.ddu.culture.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.VideoContent;

public interface ItemRepository extends JpaRepository<Item, Long> {

	List<Item> findByCategory(Category category);
	
	List<Item> findByCategoryAndGenre(Category category, String genre);
	
	List<Item> findByCategoryAndGenreIn(Category category, List<String> genres);
	
	List<Item> findTop10ByCategoryOrderByCreatedAtDesc(Category category);
	
	boolean existsByTitle(String title);
	
	long countByCategory(Category category);
	
	@Query("SELECT v FROM VideoContent v WHERE v.genre IN :genres")
	List<VideoContent> findVideoByGenres(@Param("genres") List<String> genres);

	// 또는 카테고리도 같이 확인해야 한다면
	@Query("SELECT v FROM VideoContent v WHERE v.category = :category AND v.genre IN :genres")
	List<VideoContent> findVideoByCategoryAndGenres(@Param("category") Category category, @Param("genres") List<String> genres);

}
