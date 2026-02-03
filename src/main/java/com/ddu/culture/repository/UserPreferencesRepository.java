package com.ddu.culture.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserPreferences;
import java.util.List;
import java.util.Optional;


public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long>{

	List<UserPreferences> findByUser(User user);
	
	Optional<UserPreferences> findByUserAndCategoryAndGenre(User user, String category, String genre);
}
