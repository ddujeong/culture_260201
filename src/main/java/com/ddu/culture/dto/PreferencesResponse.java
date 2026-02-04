package com.ddu.culture.dto;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.UserPreferences;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PreferencesResponse {

    private Long preferenceId;
    private Long itemId;
    private Category category;
    private String genre;
    
    public static PreferencesResponse from(UserPreferences up) {
        PreferencesResponse dto = new PreferencesResponse();
        dto.preferenceId = up.getId();
        dto.itemId = up.getItem().getId();
        dto.category = up.getCategory();
        dto.genre = up.getGenre();
        return dto;
    }

}
