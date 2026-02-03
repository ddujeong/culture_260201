package com.ddu.culture.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserAction;

public interface UserActionRepository extends JpaRepository<UserAction, Long>{

	List<UserAction> findByUser(User user);
	
	List<UserAction> findByUserAndActionType(User user, String actionType);
}
