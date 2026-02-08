package com.ddu.culture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserPreferences;
import java.util.List;
import java.util.Optional;


public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long>{

	List<UserPreferences> findByUserId(Long userId);
	
	List<UserPreferences> findByUserIdAndCategory(Long userId, String category);
	
	Optional<UserPreferences> findByUserIdAndCategoryAndGenre(Long userId, Category category, String genre);

	@Query("SELECT p.category FROM UserPreferences p WHERE p.user.id = :userId GROUP BY p.category ORDER BY COUNT(p) DESC")
	List<String> findFavoriteCategory(@Param("userId") Long userId);
	
	@Query("""
		    SELECT p.genre, COUNT(p)
		    FROM UserPreferences p
		    WHERE p.user.id = :userId
		    GROUP BY p.genre
		""")
		List<Object[]> countGenreByUserId(@Param("userId") Long userId);


}
