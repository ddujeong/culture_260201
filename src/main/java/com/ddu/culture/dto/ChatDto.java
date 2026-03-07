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
    }

    @Getter
    @AllArgsConstructor
    public static class Response {
        private String answer;
    }
}