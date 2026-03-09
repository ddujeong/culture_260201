package com.ddu.culture.service;

import com.ddu.culture.entity.ActionType;
import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.UserAction;
import com.ddu.culture.entity.UserPreferences;
import com.ddu.culture.entity.UserReview;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.repository.UserActionRepository;
import com.ddu.culture.repository.UserPreferencesRepository;
import com.ddu.culture.repository.UserRepository;
import com.ddu.culture.repository.UserReviewRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class GeminiService {

    private final Client geminiClient;
    private final ItemRepository itemRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserActionRepository userActionRepository;
    private final UserReviewRepository userReviewRepository;
    private final UserRepository userRepository;

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
    private Category inferCategoryFromMessage(String message) {

        String msg = message.toLowerCase();

        if (msg.contains("애니") || msg.contains("애니메이션") || msg.contains("지브리") || msg.contains("픽사"))
            return Category.ANIMATION;

        if (msg.contains("영화"))
            return Category.MOVIE;

        if (msg.contains("드라마"))
            return Category.DRAMA;

        if (msg.contains("예능"))
            return Category.TV_SHOW;

        if (msg.contains("책") || msg.contains("소설"))
            return Category.BOOK;

        if (msg.contains("노래") || msg.contains("음악"))
            return Category.MUSIC;

        return null;
    }
    public String getCultureRecommendation(Long userId, String userMessage, Long viewedItemId) {
    	Category detectedCategory = null;
    	if (viewedItemId != null) {
            saveUserAction(userId, viewedItemId, ActionType.WATCHED);
            detectedCategory = itemRepository.findById(viewedItemId)
                    .map(Item::getCategory)
                    .orElse(null);
        }
    	// 2. 취향 데이터 로드
        List<UserPreferences> prefs = userPreferencesRepository.findByUserId(userId);
        String preferredGenres = prefs.stream().filter(p -> p.getWeight() > 0)
                .map(UserPreferences::getGenre).distinct().collect(Collectors.joining(", "));

        List<UserReview> reviews = userReviewRepository.findByUserId(userId);
        List<String> dislikeList = prefs.stream().filter(p -> p.getWeight() < 0)
                .map(UserPreferences::getGenre).collect(Collectors.toCollection(ArrayList::new));
        dislikeList.addAll(reviews.stream().filter(r -> r.getRating() <= 2)
                .map(r -> r.getItem().getGenre()).toList());
        
        // AI에게 전달할 싫어하는 장르 문자열
        String dislikedGenres = dislikeList.stream().distinct().collect(Collectors.joining(", "));
     // 3. 카테고리 결정 (이전 로직 동일)
        if (detectedCategory == null) detectedCategory = inferCategoryFromMessage(userMessage);
        if (detectedCategory == null) {
            detectedCategory = userActionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                    .map(action -> action.getItem().getCategory()).orElse(Category.ANIMATION);
        }

        // 2. 결정된 카테고리로 아이템 풀(Pool) 생성
        List<Item> pool = itemRepository.findByCategory(detectedCategory);
        if (pool.isEmpty()) {
            pool = itemRepository.findTop100ByOrderByCreatedAtDesc();
        }
        // 3. 이미 본 아이템 제외
        List<Long> viewedItemIds = userActionRepository.findByUserIdAndActionType(userId, ActionType.WATCHED)
        		.stream()
        		.map(action -> action.getItem().getId())
        		.collect(Collectors.toList());
        
     // 4. 추천 후보 생성
        List<Item> candidates = pool.stream()
                .filter(item -> !viewedItemIds.contains(item.getId())) // 1. 이미 본 것 제외
                .filter(item -> !dislikeList.contains(item.getGenre())) // 2. 싫어하는 장르 원천 봉쇄
                .collect(Collectors.toList());
        // 후보가 너무 적으면 fallback
        if (candidates.size() < 10) {
            candidates = pool;
        }
        Collections.shuffle(candidates);
        List<Item> finalCandidates = candidates.stream().limit(30).toList();
        // 5. knowledge base 생성
        String knowledgeBase = finalCandidates.stream()
                .map(i -> String.format("[ID:%d] %s | genre:%s | rating:%.1f | %s",
                        i.getId(), i.getTitle(), i.getGenre(), i.getExternalRating(), i.getDescription()))
                .collect(Collectors.joining("\n"));
        List<UserAction> recentRecs = userActionRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
        String recentContext = recentRecs.stream()
                .map(ua -> String.format("[%d] %s", ua.getItem().getId(), ua.getItem().getTitle()))
                .collect(Collectors.joining(", "));

        // 5. 프롬프트 수정 (거짓말 금지 명령 추가)
        String prompt = String.format(
        		"### [기억: 네가 방금 추천했던 목록] ###\n" +
        	            "%s\n\n" +
        	            "### [규칙] ###\n" +
        	            "1. 사용자가 '그거 봤어', '방금 말한 거 알아'라고 하면 위 [기억] 목록에서 해당 작품의 ID를 찾아 반드시 viewed_item_titles에 넣어라.\n" +
        	            "2. [DATABASE] 목록에 없는 작품이라도 [기억]에 있다면 그 ID를 써라.\n" +
        	            
                "### [SYSTEM: JSON ONLY] ###\n" +
                "너는 사용자의 취향을 분석하여 오직 JSON 데이터만 출력하는 기계다.\n" +
                "인사말, 서론, 설명, 마크다운(```json 등)은 절대 포함하지 말고 오직 { } 본체만 출력해라.\n\n" +

				"### [작동 규칙] ###\n" +
				"1. 질문이 추천 요청이거나, 특정 작품에 대한 정보 요청(예: '~에 대해 알려줘', '줄거리가 뭐야?')이라면 성실하게 답변해라.\n" +
				"2. 특정 작품의 상세 정보를 물어볼 때는 해당 작품의 정보를 'message'에 자세히 설명하고, 'items'는 [] (빈 배열)로 보내라.\n" +
				"3. 3. 사용자가 DATABASE에 없는 작품을 말하더라도 카테고리와 분위기를 추론해서 DATABASE 안에서 가장 비슷한 작품을 추천해라.\n" +
				"4. 인사나 잡담에는 철벽을 치되, 사용자가 특정 작품을 '봤다'거나 '취향이 아니다'라고 피드백하는 것은 추천을 위한 중요한 맥락으로 간주해라.\n" +				
				"5. 추천 요청이나 작품 피드백(봤다, 싫다 등)을 받으면, 반드시 [DATABASE] 내에서 가장 적합한 새로운 작품 3가지를 골라 'items'를 즉시 채워라. '봤군요'라고 답변만 하고 멈추지 마라.\n" +		
				"6. ★필터링 우선순위★: 1순위(사용자의 명시적 조건: 평점, 특정 장르) > 2순위(금지 장르 제외) > 3순위(사용자 선호 장르).\n" +
				"7. 만약 사용자가 '평점 4점 이상'을 요구했는데 DATABASE에 해당되는 것이 없다면, 억지로 애니메이션을 꺼내지 마라.\n" +
				"8. 조건에 맞는 게 없을 때는 '현재 목록에는 평점 4점 이상인 작품이 없어요. 대신 가장 높은 평점의 다른 작품들을 보여드릴까요?'라고 물어봐라.\n" +
				"9. ★ID 추출 필독★: 사용자가 작품을 '봤다'고 하면, [DATABASE] 목록에서 해당 제목을 찾아 반드시 'viewed_item_titles' 배열에 넣어라. (예: '인사이드 아웃 봤어' -> [\"인사이드 아웃\"])."+
				"10. 사용자가 '추천해준 거 다 봤어'라고 하면, 바로 직전 대화([기억])에서 네가 추천했던 작품들의 제목을 'viewed_item_titles'에 넣어라.\n" +
                "11. 만약 추천할 수 있는 새로운 작품이 하나도 없다면, '모든 작품을 다 보셨네요! 대단해요!'라고 칭찬하고 이전에 보셨던 작품 중 다시 볼만한 것을 추천해라.\n\n" +
                "12. 사용자가 특정 카테고리(애니, 영화, 드라마 등)를 요청했다면 이후 추천에서도 반드시 같은 카테고리만 추천해라. 다른 카테고리는 절대 추천하지 마라." + 
				
				"### [DATA 가이드] ###\n" +
				"1. DATABASE의 '평점'은 10점 만점 기준이다. (예: 7.611)\n" +
				"2. 만약 사용자가 '별점 3점' 혹은 '평점 3점' 이상을 요구한다면, 이는 5점 만점 기준일 확률이 높다.\n" +
				"3. 사용자의 요구치를 10점 만점으로 환산(사용자 점수 * 2)해서 계산하거나, 데이터의 수치(7.6점 등)를 그대로 읽고 판단해라.\n" +
				"4. 평점이 7점 이상이면 매우 우수한 작품으로 간주하고 추천해라.\n" +
				"5. 사용자가 '픽사 같은', '지브리 같은'이라고 말하는 것은 특정 제작사를 찾으라는 게 아니라, 그와 유사한 '분위기(따뜻한, 모험, 상상력)'의 작품을 [DATABASE]에서 찾으라는 뜻이다.\n" +
				"6. 만약 [DATABASE]에 '토이스토리'가 없더라도, [DATABASE] 내의 다른 애니메이션 중 모험과 감동이 있는 작품을 골라 추천해라. 절대 '없다'고 단정 짓지 마라.\n" +
				
                "### [DATABASE] ###\n" +
                "추천 가능한 아이템 목록:\n%s\n\n" +

                "### [USER INFO] ###\n" +
                "- 금지 장르(절대 제외): [%s]\n" +
                "- 선호 장르: [%s]\n" +
                "- 질문: \"%s\"\n\n" +

                "### [OUTPUT FORMAT] ###\n" +
                "{\n" +
                "  \"message\": \"질문에 대한 짧고 친절한 답변 (특수문자 없이,봤다고 한 작품은 '시청 목록에 추가할게요' 등의 언급 포함)\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"id\": 아이템ID(숫자),\n" +
                "      \"title\": \"제목\",\n" +
                "      \"genre\": \"장르\",\n" +
                "      \"reason\": \"추천 이유 (한 문장)\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"viewed_item_titles\": [\"사용자가 봤다고 언급한 작품 제목들\"]\n" +
                "}",
                recentContext,knowledgeBase, dislikedGenres, preferredGenres, userMessage
             );

        // 6. Gemini 호출 (기존 호출 로직 유지)
        try {
        	GenerateContentResponse response = geminiClient.models.generateContent("gemini-2.5-flash", prompt, null);
            String rawResponse = response.text().trim();
            String cleanJson = rawResponse.replaceAll("(?s)^.*?(\\{.*\\}).*?$", "$1");
            try {
                // 1. JSON 응답 파싱 (Jackson ObjectMapper 사용)
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(cleanJson);
                
                // 2. viewed_item_ids 필드가 있는지 확인
                JsonNode viewedTitlesNode = root.get("viewed_item_titles");
                System.out.println("🤖 AI 응답 원문: " + cleanJson);
                
                if (viewedTitlesNode != null && viewedTitlesNode.isArray()) {
                    for (JsonNode titleNode : viewedTitlesNode) {
                        String title = titleNode.asText().trim();
                        itemRepository.findByTitle(title).ifPresent(item -> {
                        	saveUserAction(userId, item.getId(), ActionType.WATCHED);
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ 시청 완료 데이터 처리 중 오류: " + e.getMessage());
            }
            return cleanJson;
        } catch (Exception e) {
        	System.out.println(e.getMessage());
            return "{\"message\": \"데이터 분석 중 오류가 발생했어요.\", \"items\": []}";
        }
    }
    private void saveUserAction(Long userId, Long itemId, ActionType actionType) {
        userRepository.findById(userId).ifPresent(user -> {
            itemRepository.findById(itemId).ifPresent(item -> {
                // 중복 저장 방지 (이미 본 항목이면 저장 안 함 - 선택사항)
                boolean alreadyExists = userActionRepository.findByUserIdAndActionType(userId, actionType)
                        .stream()
                        .anyMatch(action -> action.getItem().getId().equals(itemId));

                if (!alreadyExists) {
                    UserAction action = new UserAction();
                    action.setUser(user);
                    action.setItem(item);
                    action.setActionType(actionType);
                    userActionRepository.save(action);
                    System.out.println("💾 DB 저장 완료: 유저(" + userId + ")가 " + item.getTitle() + "을(를) " + actionType + " 함");
                }
            });
        });
    }
}