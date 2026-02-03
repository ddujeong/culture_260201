package com.ddu.culture.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ddu.culture.dto.PreferencesRequest;
import com.ddu.culture.dto.SignupRequest;
import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserPreferences;
import com.ddu.culture.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;
	
	// 회원가입
	@PostMapping("/signup")
	public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
		User user = userService.signup(request);
		return ResponseEntity.ok(user);
	}
	
	// 취향 등록
	@PostMapping("/preferences")
	public ResponseEntity<?> registerPreferences(@RequestBody PreferencesRequest request) {
		List<UserPreferences> prefs = userService.registerPreferences(request);
		return ResponseEntity.ok(prefs);
	}
}
