package com.ddu.culture.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ddu.culture.dto.ActionStatusResponse;
import com.ddu.culture.entity.ActionType;
import com.ddu.culture.service.UserActionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/action")
@RequiredArgsConstructor
public class UserActionController {

    private final UserActionService userActionService;

    // 보고싶어요 등록
    @PostMapping("/{userId}/reserve")
    public ResponseEntity<?> reserve(
        @PathVariable(name = "userId") Long userId,
        @RequestParam(name = "itemId") Long itemId
    ) {
        userActionService.reserveItem(userId, itemId);
        return ResponseEntity.ok().build();
    }

    // 보고싶은 목록 조회
    @GetMapping("/{userId}/reserve")
    public ResponseEntity<?> getReserved(
        @PathVariable(name = "userId") Long userId
    ) {
        return ResponseEntity.ok(
            userActionService.getReservedItems(userId)
        );
    }
 // 본 콘텐츠
    @GetMapping("/{userId}/watched")
    public ResponseEntity<?> getWatched(
        @PathVariable(name = "userId") Long userId
    ) {
        return ResponseEntity.ok(
            userActionService.getWatchedItems(userId)
        );
    }
 // 시청 완료 처리
    @PostMapping("/{userId}/watched")
    public ResponseEntity<?> markAsWatched(
        @PathVariable(name = "userId") Long userId,
        @RequestParam(name = "itemId") Long itemId
    ) {
        userActionService.markAsWatched(userId, itemId);
        return ResponseEntity.ok().build();
    }
 // 리뷰 완료 처리
    @PostMapping("/{userId}/reviewed")
    public ResponseEntity<?> markAsReviewed(
            @PathVariable(name = "userId") Long userId,
            @RequestParam(name = "itemId") Long itemId
    ) {
        userActionService.markAsReviewed(userId, itemId);
        return ResponseEntity.ok().build();
    }
 // 특정 유저-아이템 상태 조회
    @GetMapping("/{userId}/item/{itemId}")
    public ResponseEntity<ActionStatusResponse> getActionStatus(
            @PathVariable(name = "userId") Long userId,
            @PathVariable(name = "itemId") Long itemId
    ) {
        String status = userActionService.getActionStatus(userId, itemId);
        if (status == null) status = "NONE"; // null 방지

        return ResponseEntity.ok(new ActionStatusResponse(status));
    }

}

