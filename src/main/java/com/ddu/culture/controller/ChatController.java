package com.ddu.culture.controller;

import com.ddu.culture.dto.ChatDto;
import com.ddu.culture.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final GeminiService geminiService;

    @PostMapping("/ask")
    public ResponseEntity<ChatDto.Response> askToDdu(@RequestBody ChatDto.Request request, @RequestParam(name = "userId") Long userId) {
        // 유저가 보낸 메시지 추출
        String userMessage = request.getMessage();
        
        // GeminiService를 통해 우리 DB 기반 추천 답변 생성
        String aiAnswer = geminiService.getCultureRecommendation(userId,userMessage);
        
        // 응답 객체에 담아서 반환
        return ResponseEntity.ok(new ChatDto.Response(aiAnswer));
    }
}