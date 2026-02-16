package com.ddu.culture.dto;

import java.util.List;

import com.ddu.culture.entity.Category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemScoreDto {

	private Long itemId;
    private String title;
    private Category category;
    private double score;
    private List<String> reasons; // 추천 이유 메시지
}
