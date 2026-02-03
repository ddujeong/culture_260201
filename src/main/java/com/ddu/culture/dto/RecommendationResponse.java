package com.ddu.culture.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendationResponse {
	private List<RecommendationDto> items;

}
