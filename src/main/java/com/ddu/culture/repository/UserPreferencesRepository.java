package com.ddu.culture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ddu.culture.entity.UserPreferences;
import java.util.List;


public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long>{

	List<UserPreferences> findByUserId(Long userId);
	
	@Query("SELECT p.category FROM UserPreferences p WHERE p.user.id = :userId GROUP BY p.category ORDER BY COUNT(p) DESC")
	List<String> findFavoriteCategory(@Param("userId") Long userId);
	
}
