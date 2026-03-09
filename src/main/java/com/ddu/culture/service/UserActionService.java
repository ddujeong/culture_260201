package com.ddu.culture.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ddu.culture.dto.UserActionResponse;
import com.ddu.culture.entity.ActionType;
import com.ddu.culture.entity.UserAction;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.repository.UserActionRepository;
import com.ddu.culture.repository.UserRepository;
import com.ddu.culture.repository.UserReviewRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserActionService {

	private final UserActionRepository userActionRepository;
	private final UserReviewRepository userReviewRepository;
	private final UserRepository userRepository;
	private final ItemRepository itemRepository;
	
	public List<UserActionResponse> getReservedItems(Long userId) {
	    return userActionRepository
	        .findByUserIdAndActionType(userId, ActionType.RESERVE)
	        .stream()
	        .map(UserActionResponse::from)
	        .toList();
	}

	
	public void reserveItem(Long userId, Long itemId) {

	    if (userActionRepository.existsByUserIdAndItemIdAndActionType(
	            userId, itemId, ActionType.RESERVE)) {
	        return;
	    }

	    UserAction action = new UserAction();
	    action.setUser(userRepository.getReferenceById(userId));
	    action.setItem(itemRepository.getReferenceById(itemId));
	    action.setActionType(ActionType.RESERVE);
	    action.setCreatedAt(LocalDateTime.now());

	    userActionRepository.save(action);
	}
	public void markAsWatched(Long userId, Long itemId) {

	    // 보고싶어요 → 삭제
	    userActionRepository.deleteByUserIdAndItemIdAndActionType(
	        userId, itemId, ActionType.RESERVE
	    );
	    if (userActionRepository.existsByUserIdAndItemIdAndActionType(
	            userId, itemId, ActionType.WATCHED)) {
	        return;
	    }
	    // WATCHED 추가
	    UserAction watched = new UserAction();
	    watched.setUser(userRepository.getReferenceById(userId));
	    watched.setItem(itemRepository.getReferenceById(itemId));
	    watched.setActionType(ActionType.WATCHED);
	    watched.setCreatedAt(LocalDateTime.now());

	    userActionRepository.save(watched);
	}
	// 🔹 본 콘텐츠
    public List<UserActionResponse> getWatchedItems(Long userId) {
        return userActionRepository
            .findByUserIdAndActionType(userId, ActionType.WATCHED)
            .stream()
            .map(UserActionResponse::from)
            .toList();
    }
    public void markAsReviewed(Long userId, Long itemId) {
        if (userActionRepository.existsByUserIdAndItemIdAndActionType(userId, itemId, ActionType.REVIEWED)) {
            return;
        }

        UserAction reviewed = new UserAction();
        reviewed.setUser(userRepository.getReferenceById(userId));
        reviewed.setItem(itemRepository.getReferenceById(itemId));
        reviewed.setActionType(ActionType.REVIEWED);
        reviewed.setCreatedAt(LocalDateTime.now());

        userActionRepository.save(reviewed);
    }

    public String getActionStatus(Long userId, Long itemId) {

        if (userReviewRepository.existsByUserIdAndItemId(userId, itemId)) {
            return "REVIEWED";
        }
        if (userActionRepository.existsByUserIdAndItemIdAndActionType(userId, itemId, ActionType.WATCHED)) {
            return "WATCHED";
        }
        if (userActionRepository.existsByUserIdAndItemIdAndActionType(userId, itemId, ActionType.RESERVE)) {
            return "RESERVE";
        }

        return null;
    }
   
}
