package com.ddu.culture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserReview;
import java.util.List;
import java.util.Optional;


public interface UserReviewRepository extends JpaRepository<UserReview, Long> {

	List<UserReview> findByUserId(Long userId);
	
	Optional<UserReview> findByUserAndItem(User user, Item item);
	
	List<UserReview> findByItem(Item item);
	
    List<UserReview> findByItemIdOrderByCreatedAtDesc(Long itemId);

    @Query("SELECT AVG(r.rating) FROM UserReview r WHERE r.user.id = :userId")
    Double findAvgRatingByUserId(@Param("userId") Long userId);

    Long countByUserId(Long userId);

}
