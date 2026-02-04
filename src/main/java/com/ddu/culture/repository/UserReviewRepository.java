package com.ddu.culture.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserReview;
import java.util.List;
import java.util.Optional;


public interface UserReviewRepository extends JpaRepository<UserReview, Long> {

	List<UserReview> findByUser(User user);
	
	Optional<UserReview> findByUserAndItem(User user, Item item);
	
	List<UserReview> findByItem(Item item);
	
    List<UserReview> findByItemIdOrderByCreatedAtDesc(Long itemId);

}
