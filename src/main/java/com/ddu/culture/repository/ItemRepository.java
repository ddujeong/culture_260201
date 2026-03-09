package com.ddu.culture.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

	Page<Item> findByCategory(Category category, Pageable pageable);
	List<Item> findByCategory(Category category);
	
	List<Item> findByCategoryAndGenreIn(Category category, List<String> genres);
	
	List<Item> findTop10ByCategoryOrderByCreatedAtDesc(Category category);
	
	boolean existsByTitle(String title);
	
	Optional<Item> findByTitle(String title);
	
    @Query("SELECT i FROM Item i WHERE TYPE(i) = :type")
    List<Item> findByItemType(@Param("type") Class<? extends Item> type);
    
    @Query("SELECT i FROM Item i WHERE TYPE(i) = :type AND i.category = :category")
    List<Item> findByItemTypeAndCategory(
        @Param("type") Class<? extends Item> type, 
        @Param("category") Category category
    );
    
    boolean existsByCategoryAndUpdatedAtAfter(Category category, LocalDateTime date);
    
    List<Item> findTop100ByOrderByCreatedAtDesc();
    
    @Query("""
    		SELECT ua.item.id, COUNT(ua)
    		FROM UserAction ua
    		WHERE ua.item.id IN :itemIds
    		GROUP BY ua.item.id
    		""")
    		List<Object[]> countViewsByItemIds(List<Long> itemIds);
    
}
