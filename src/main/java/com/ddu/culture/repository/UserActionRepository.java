package com.ddu.culture.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ddu.culture.entity.ActionType;
import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserAction;

public interface UserActionRepository extends JpaRepository<UserAction, Long>{

	List<UserAction> findByUser(User user);
	
	List<UserAction> findByUserAndActionType(User user, ActionType actionType);
	
	List<UserAction> findByUserId(Long userId);

	List<UserAction> findByUserIdAndActionType(Long userId, ActionType actionType);

	List<UserAction> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

	List<UserAction> findTop20ByUserIdAndActionTypeOrderByCreatedAtDesc(
	    Long userId,
	    ActionType actionType
	);
	
	boolean existsByUserIdAndItemIdAndActionType(
	        Long userId,
	        Long itemId,
	        ActionType actionType
	    );
	void deleteByUserIdAndItemIdAndActionType(
		    Long userId,
		    Long itemId,
		    ActionType actionType
		);
	long countByItemId(Long itemId);
	
	// --- 장르별 count 집계 (UserAction 기준) ---
    @Query("SELECT ua.item.genre, COUNT(ua) " +
           "FROM UserAction ua " +
           "WHERE ua.user.id = :userId " +
           "AND ua.actionType = :actionType " +
           "GROUP BY ua.item.genre")
    List<Object[]> countGenresByUser(@Param("userId") Long userId,
                                     @Param("actionType") ActionType actionType);
}
