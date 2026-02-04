package com.ddu.culture.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ddu.culture.dto.LoginRequest;
import com.ddu.culture.dto.PreferencesRequest;
import com.ddu.culture.dto.PreferencesResponse;
import com.ddu.culture.dto.SignupRequest;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.User;
import com.ddu.culture.entity.UserPreferences;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.repository.UserPreferencesRepository;
import com.ddu.culture.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional 
public class UserService {

	private final UserRepository userRepository;
	private final UserPreferencesRepository userPreferencesRepository;
	private final ItemRepository itemRepository;
	
	// 회원가입
	public User signup(SignupRequest request) {
		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setPassword(request.getPassword());
		return userRepository.save(user);
	}
	
	// 취향등록
	public List<PreferencesResponse> registerPreferences(PreferencesRequest request) {
		User user = userRepository.findById(request.getUserId())
				.orElseThrow(() -> new RuntimeException("user not found"));
		
		List<Item> selectedItems = itemRepository.findAllById(request.getItemsIds());
		
		List<UserPreferences> prefs = selectedItems.stream()
				.map(item -> {
					UserPreferences up = new UserPreferences();
					up.setUser(user);
					up.setItem(item);
					up.setCategory(item.getCategory());
					up.setGenre(item.getGenre());
					up.setWeight(1);
					return up;
				}).toList();
		List<UserPreferences> saved = userPreferencesRepository.saveAll(prefs);
		
		return saved.stream()
				.map(PreferencesResponse::from)
				.toList();
	}
	
	// 임시로그인
	public User login(LoginRequest request) {
	    User user = userRepository.findByEmail(request.getEmail())
	        .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자"));

	    if (!user.getPassword().equals(request.getPassword())) {
	        throw new RuntimeException("비밀번호 불일치");
	    }

	    return user;
	}

}
