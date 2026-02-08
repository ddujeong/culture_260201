package com.ddu.culture.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionStatusResponse {
    private String status;

    public static ActionStatusResponse of(String status) {
        return new ActionStatusResponse(status);
    }
}
