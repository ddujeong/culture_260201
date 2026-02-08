package com.ddu.culture.dto;

import com.ddu.culture.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private Long id;
    private String email;
    private String username;
    
    public static LoginResponse from(User user) {
        return new LoginResponse(
            user.getId(),
            user.getEmail(),
            user.getUsername()
        );
    }

}
