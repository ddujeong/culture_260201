package com.ddu.culture.service;

import com.ddu.culture.entity.Item;
import com.ddu.culture.repository.ItemRepository;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final Client geminiClient;
    private final ItemRepository itemRepository;

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
    public String getCultureRecommendation(String userMessage) {
        // 1. DB에서 추천 후보 데이터를 가져옴 (예: 최신/인기 데이터 15개)
        // 실제로는 유저 질문 키워드에 따라 페이징이나 검색을 하면 더 좋지만, 우선은 전체에서 가져옵니다.
        List<Item> items = itemRepository.findAll(); 
        
        // 2. AI에게 전달할 데이터 텍스트 생성 (ID, 제목, 카테고리, 장르 정도만)
        String contextData = items.stream()
                .limit(15) // 너무 많으면 토큰 낭비니 적당히 끊어줍니다.
                .map(i -> String.format("[%s] 제목: %s, 장르: %s", i.getCategory(), i.getTitle(), i.getGenre()))
                .collect(Collectors.joining("\n"));

        // 3. 페르소나와 규칙을 부여한 프롬프트 작성
        String prompt = "너는 문화 콘텐츠 추천 전문가 '듀듀(DDU)'야.\n"
                + "사용자의 질문에 대해 아래 제공된 '우리 데이터베이스 목록'에 있는 작품만 우선적으로 추천해줘.\n"
                + "목록에 적절한 작품이 없다면 대중적인 작품을 추천해도 되지만, 가급적 목록을 활용해.\n"
                + "답변은 친절하고 위트 있게, 그리고 어디서 볼 수 있는지(OTT 정보 등)를 알면 같이 말해줘.\n\n"
                + "--- 우리 데이터베이스 목록 ---\n"
                + contextData + "\n\n"
                + "--- 사용자 질문 ---\n"
                + userMessage;

        try {
            // 기존에 사용하던 모델(gemini-1.5-flash 등)로 호출
            GenerateContentResponse response = geminiClient.models.generateContent("gemini-1.5-flash", prompt, null);
            return response.text().trim();
        } catch (Exception e) {
            return "죄송해요, 추천 로직을 처리하다가 살짝 어지러웠나 봐요. 잠시 후 다시 물어봐 주시겠어요? 😅";
            }
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