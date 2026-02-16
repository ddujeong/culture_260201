package com.ddu.culture.dto;


import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class ItemSummaryResponse {
    private Long id;
    private String title;
    private String img;
    private String genre;
    private Category category;

    public static ItemSummaryResponse from(Item item) {
        ItemSummaryResponse dto = new ItemSummaryResponse();
        dto.setId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setImg(item.getImg());
        dto.setGenre(item.getGenre());
        dto.setCategory(item.getCategory());
        return dto;
    }
}
