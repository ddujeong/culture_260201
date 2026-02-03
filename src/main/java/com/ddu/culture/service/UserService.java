package com.ddu.culture.service;

import java.security.cert.PKIXReason;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ddu.culture.dto.PreferencesRequest;
import com.ddu.culture.dto.SignupRequest;
import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserPreferences;
import com.ddu.culture.repository.UserPreferencesRepository;
import com.ddu.culture.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final UserPreferencesRepository userPreferencesRepository;
	
	// 회원가입
	public User signup(SignupRequest request) {
		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setPassword(request.getPassword());
		return userRepository.save(user);
	}
	
	// 취향등록
	public List<UserPreferences> registerPreferences(PreferencesRequest request) {
		User user = userRepository.findById(request.getUserId())
				.orElseThrow(() -> new RuntimeException("user not found"));
		
		List<UserPreferences> prefs = request.getPreferences().stream()
				.map(p -> {
					UserPreferences up = new UserPreferences();
					up.setUser(user);
					up.setCategory(p.getCategory());
					up.setGenre(p.getGenre());
					return up;
				}).toList();
		
		return userPreferencesRepository.saveAll(prefs);
	}
}
