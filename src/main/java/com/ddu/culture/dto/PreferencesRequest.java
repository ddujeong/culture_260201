package com.ddu.culture.dto;

import java.util.List;

import com.ddu.culture.entity.Category;

import lombok.Data;

// 취향 등록 요청 DTO
@Data
public class PreferencesRequest {

	private Long userId;
	private List<Long> itemsIds;
}
