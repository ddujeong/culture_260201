package com.ddu.culture.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final Client geminiClient;

    public Map<String, String> inferGenresBulk(List<String> trackList) {
        // 1. 요청 데이터를 하나의 문자열로 합침
        String tracksData = String.join("\n", trackList);
        
        String prompt = "음악 전문가로서 다음 노래들의 장르를 분석해줘.\n"
                + "장르 후보: [K-Pop, Pop, Hip-Hop, R&B, Rock, EDM, Jazz, Ballad]\n"
                + "응답 형식: '노래제목 === 장르'\n"
                + "설명 없이 결과만 리스트로 출력해.\n\n"
                + "노래 리스트:\n" + tracksData;

        Map<String, String> resultMap = new HashMap<>();
        try {
            GenerateContentResponse response = geminiClient.models.generateContent("gemini-2.5-flash", prompt, null);
            String resultText = response.text().trim();
            
            // 2. 응답 파싱 (제목:장르 형태)
            String[] lines = resultText.split("\n");
            for (String line : lines) {
                if (line.contains("===")) {
                    String[] parts = line.split("===", 2);
                    resultMap.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 벌크 장르 분석 실패: " + e.getMessage());
        }
        return resultMap;
    }

    private String callWithModel(String modelName, String prompt) {
        try {
            // SDK 호출 시 모델명만 넣어도 내부적으로 models/를 붙여 처리합니다.
            GenerateContentResponse response = geminiClient.models.generateContent(modelName, prompt, null);
            
            String text = response.text().trim();
            System.out.println("🤖 Gemini (" + modelName + ") 응답 성공: " + text);
            
            // 한 단어만 남기기 (AI가 마침표를 찍을 수도 있으니 처리)
            return text.replaceAll("[^a-zA-Z\\-]", "").trim();
            
        } catch (Exception e) {
            System.err.println("❌ [" + modelName + "] 호출 실패: " + e.getMessage());
            return "Pop"; 
        }
    }
}