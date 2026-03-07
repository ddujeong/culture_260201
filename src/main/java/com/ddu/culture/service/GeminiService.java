package com.ddu.culture.service;

import com.ddu.culture.entity.ActionType;
import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.User;
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
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
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
    private Category inferCategoryFromMessage(String userMessage) {
        // Enum의 모든 값을 문자열 리스트로 만듭니다 (예: MOVIE, ANIMATION...)
    	String categoryList = java.util.Arrays.stream(Category.values())
                .map(Enum::name).collect(Collectors.joining(", "));

        String prompt = String.format(
                "사용자 메시지: \"%s\"\n" +
                "주어진 카테고리: [%s]\n" +
                "규칙:\n" +
                "1. '애니', '만화', '픽사 같은', '지브리 같은' 이 포함되면 ANIMATION으로 분류해.\n" +
                "2. '영화', '무비'는 MOVIE.\n" +
                "3. 판단이 어려우면 UNKNOWN.\n" +
                "결과를 한 단어로만 응답해.", userMessage, categoryList);

        try {
            String result = callWithModel("gemini-2.0-flash", prompt).toUpperCase();
            // UNKNOWN이거나 Enum에 없는 값이면 null 반환
            return Arrays.stream(Category.values())
                    .anyMatch(c -> c.name().equals(result)) ? Category.valueOf(result) : null;
        } catch (Exception e) {
            return null; 
        }
    }
    public String getCultureRecommendation(Long userId, String userMessage) {
        // 1. 유저의 선호/비선호 장르 분석 (기존 로직 유지)
        List<UserPreferences> prefs = userPreferencesRepository.findByUserId(userId);
        String preferredGenres = prefs.stream().filter(p -> p.getWeight() > 0).map(UserPreferences::getGenre).distinct().collect(Collectors.joining(", "));
        
        List<UserReview> reviews = userReviewRepository.findByUserId(userId);
        List<String> dislikeList = new ArrayList<>(prefs.stream().filter(p -> p.getWeight() < 0).map(UserPreferences::getGenre).toList());
        dislikeList.addAll(reviews.stream().filter(r -> r.getRating() <= 2).map(r -> r.getItem().getGenre()).toList());
        String dislikedGenres = dislikeList.stream().distinct().collect(Collectors.joining(", "));

     // 2. 카테고리 추출 로직 (부정표현 대응)
        Category detectedCategory = inferCategoryFromMessage(userMessage);
        List<Item> pool;
        boolean isInstead = userMessage.contains("말고") || userMessage.contains("제외") || userMessage.contains("아니라");

        if (detectedCategory != null) {
            // AI가 카테고리를 명확히 짚어냈을 때 (예: "지브리 같은 애니")
            pool = itemRepository.findByCategory(detectedCategory);
        } else {
            // AI가 카테고리를 못 찾았을 때 (예: "그건 봤어", "취향 아냐")
            // 유저가 최근에 본(WATCHED) 아이템의 카테고리를 따라가는 것이 가장 자연스럽습니다.
            Category lastCategory = userActionRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                    .map(action -> action.getItem().getCategory())
                    .orElse(Category.MOVIE); // 데이터가 아예 없으면 기본값 MOVIE
            
            pool = itemRepository.findByCategory(lastCategory);
        }

        // 데이터가 부족할 경우를 대비한 안전장치
        if (pool.isEmpty()) {
            pool = itemRepository.findTop100ByOrderByCreatedAtDesc();
        }

        // 3. 이미 본 아이템 제외
        List<Long> viewedItemIds = userActionRepository.findByUserId(userId).stream()
                .map(action -> action.getItem().getId()).collect(Collectors.toList());

        // 4. 지식 베이스 구축 (ID와 카테고리를 명시해서 AI가 속이지 못하게 함)
        String knowledgeBase = pool.stream()
                .filter(item -> !viewedItemIds.contains(item.getId()))
                .limit(60)
                .map(i -> String.format("ID: %d, 카테고리: %s, 제목: %s, 장르: %s, 평점: %.1f, 요약: %s", 
                        i.getId(), i.getCategory(), i.getTitle(), i.getGenre(), 
                        i.getExternalRating(), // 이 부분이 추가되어야 합니다!
                        i.getDescription()))
                  .collect(Collectors.joining("\n"));

        // 5. 프롬프트 수정 (거짓말 금지 명령 추가)
        String prompt = String.format(
                "### [SYSTEM: JSON ONLY] ###\n" +
                "너는 사용자의 취향을 분석하여 오직 JSON 데이터만 출력하는 기계다.\n" +
                "인사말, 서론, 설명, 마크다운(```json 등)은 절대 포함하지 말고 오직 { } 본체만 출력해라.\n\n" +

				"### [작동 규칙] ###\n" +
				"1. 질문이 추천 요청이거나, 특정 작품에 대한 정보 요청(예: '~에 대해 알려줘', '줄거리가 뭐야?')이라면 성실하게 답변해라.\n" +
				"2. 특정 작품의 상세 정보를 물어볼 때는 해당 작품의 정보를 'message'에 자세히 설명하고, 'items'는 [] (빈 배열)로 보내라.\n" +
				"3. [DATABASE]에 있는 작품이라면 아는 척을 하고, 없는 작품에 대해 물으면 모른다고 정직하게 답해라.\n" +
				"4. 인사나 잡담에는 철벽을 치되, 사용자가 특정 작품을 '봤다'거나 '취향이 아니다'라고 피드백하는 것은 추천을 위한 중요한 맥락으로 간주해라.\n" +				
				"5. 추천 요청이나 작품 피드백(봤다, 싫다 등)을 받으면, 반드시 [DATABASE] 내에서 가장 적합한 새로운 작품 3가지를 골라 'items'를 즉시 채워라. '봤군요'라고 답변만 하고 멈추지 마라.\n" +		
				"6. ★필터링 우선순위★: 1순위(사용자의 명시적 조건: 평점, 특정 장르) > 2순위(금지 장르 제외) > 3순위(사용자 선호 장르).\n" +
				"7. 만약 사용자가 '평점 4점 이상'을 요구했는데 DATABASE에 해당되는 것이 없다면, 억지로 애니메이션을 꺼내지 마라.\n" +
				"8. 조건에 맞는 게 없을 때는 '현재 목록에는 평점 4점 이상인 작품이 없어요. 대신 가장 높은 평점의 다른 작품들을 보여드릴까요?'라고 물어봐라.\n" +
				"9. ★시청 완료 처리★: 사용자가 '이미 봤어', '다 봤어', 'OO은 봤어'라고 말하면, [DATABASE]에서 해당 작품의 ID를 찾아 'viewed_item_ids' 배열에 담아라.\n" +
			    "10. 사용자가 '추천해준 거 다 봤어'라고 하면, 바로 직전 대화에서 네가 추천했던 작품들의 ID를 'viewed_item_ids'에 넣어라.\n" +
                "11. 만약 추천할 수 있는 새로운 작품이 하나도 없다면, '모든 작품을 다 보셨네요! 대단해요!'라고 칭찬하고 이전에 보셨던 작품 중 다시 볼만한 것을 추천해라.\n\n" +
				
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
                "  \"viewed_item_ids\": [사용자가 시청 완료했다고 판단되는 작품의 ID 리스트 (숫자 배열)]\n" +
                "}",
                knowledgeBase, dislikedGenres, preferredGenres, userMessage
             );

        // 6. Gemini 호출 (기존 호출 로직 유지)
        try {
        	GenerateContentResponse response = geminiClient.models.generateContent("gemini-2.5-flash", prompt, null);
            String rawResponse = response.text().trim();
         // 로그로 확인해보세요!
            System.out.println("AI에게 전달되는 지식 베이스: " + knowledgeBase);
            // 💡 마크다운 코드 블록(```json )이 포함되어 있다면 제거하는 정규식
            String cleanJson = rawResponse.replaceAll("(?s)^.*?(\\{.*\\}).*?$", "$1");
            try {
                // 1. JSON 응답 파싱 (Jackson ObjectMapper 사용)
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(cleanJson);
                
                // 2. viewed_item_ids 필드가 있는지 확인
                JsonNode viewedIdsNode = root.get("viewed_item_ids");
                
                if (viewedIdsNode != null && viewedIdsNode.isArray()) {
                    for (JsonNode idNode : viewedIdsNode) {
                        Long itemId = idNode.asLong();
                        
                        // 3. 이미 본 목록에 있는지 중복 체크 후 저장
                        // viewedItemIds는 위에서 이미 조회한 List<Long>입니다.
                        if (!viewedItemIds.contains(itemId)) {
                            Item item = itemRepository.findById(itemId).orElse(null);
                            if (item != null) {
                                User user = userRepository.findById(userId).orElse(null); 
                                if (user != null) {
                                    UserAction action = new UserAction();
                                    action.setUser(user);
                                    action.setItem(item);
                                    action.setActionType(ActionType.WATCHED); // 또는 시청 완료를 나타내는 상수/Enum
                                    userActionRepository.save(action);
                                    System.out.println("✅ 아이템 ID " + itemId + " 시청 완료 처리됨");
                                }
                            }
                        }
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