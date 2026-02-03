package com.ddu.culture.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserPreferences;
import java.util.List;
import java.util.Optional;


public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long>{

	List<UserPreferences> findByUserId(Long userId);
	
	List<UserPreferences> findByUserIdAndCategory(Long userId, String category);
	
	Optional<UserPreferences> findByUserAndCategoryAndGenre(User user, String category, String genre);
}
