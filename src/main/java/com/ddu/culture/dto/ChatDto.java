package com.ddu.culture.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatDto {
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String message;
        private Long viewedItemId;
    }

    @Getter
    @AllArgsConstructor
    public static class Response {
        private String answer;
    }
}