package com.ddu.culture.dto;

import java.time.LocalDateTime;

import com.ddu.culture.entity.ActionType;
import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.UserAction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserActionResponse {

    private Long actionId;
    private Long itemId;
    private String title;
    private Category category;
    private ActionType actionType;
    private LocalDateTime createdAt;

    public static UserActionResponse from(UserAction action) {
        return new UserActionResponse(
            action.getId(),
            action.getItem().getId(),
            action.getItem().getTitle(),
            action.getItem().getCategory(),
            action.getActionType(),
            action.getCreatedAt()
        );
    }
}
