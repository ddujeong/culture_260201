package com.ddu.culture.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserAction;

public interface UserActionRepository extends JpaRepository<UserAction, Long>{

	List<UserAction> findByUser(User user);
	
	List<UserAction> findByUserAndActionType(User user, String actionType);
	
	List<UserAction> findByUserId(Long userId);

	List<UserAction> findByUserIdAndActionType(Long userId, String actionType);

	List<UserAction> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

	List<UserAction> findTop20ByUserIdAndActionTypeOrderByCreatedAtDesc(
	    Long userId,
	    String actionType
	);

	
}
