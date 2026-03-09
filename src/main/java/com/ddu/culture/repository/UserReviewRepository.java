package com.ddu.culture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.UserReview;
import java.util.List;


public interface UserReviewRepository extends JpaRepository<UserReview, Long> {

	List<UserReview> findByUserId(Long userId);
	
    List<UserReview> findByItemIdOrderByCreatedAtDesc(Long itemId);

    @Query("SELECT AVG(r.rating) FROM UserReview r WHERE r.user.id = :userId")
    Double findAvgRatingByUserId(@Param("userId") Long userId);

    Long countByUserId(Long userId);
    
    @Query("SELECT AVG(r.rating) FROM UserReview r WHERE r.item.id = :itemId")
    Double findAvgRatingByItemId(@Param("itemId") Long itemId);

    @Query("SELECT r.rating, COUNT(r) " +
    	       "FROM UserReview r " +
    	       "WHERE r.user.id = :userId " +
    	       "GROUP BY r.rating " +
    	       "ORDER BY r.rating DESC")
    	List<Object[]> countRatingByUserId(@Param("userId") Long userId);

    	// 유저 + 카테고리별 리뷰 조회
    	@Query("SELECT r FROM UserReview r WHERE r.user.id = :userId AND r.item.category = :category ORDER BY r.createdAt DESC")
    	List<UserReview> findByUserIdAndCategory(@Param("userId") Long userId,
    	                                         @Param("category") Category category);

        boolean existsByUserIdAndItemId(Long userId, Long itemId);

}
