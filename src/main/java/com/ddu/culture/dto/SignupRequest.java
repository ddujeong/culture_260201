package com.ddu.culture.dto;

import lombok.Data;

// 회원가입 요청 DTO
@Data
public class SignupRequest {

	private String username;
    private String email;
    private String password;
}
