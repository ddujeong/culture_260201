package com.ddu.culture.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.StaticContent;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.repository.StaticContentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AladinService {

    @Value("${aladin.api.key}")
    private String apiKey;

    private final StaticContentRepository staticContentRepository; // StaticContent 전용 사용
    private final ItemRepository itemRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void fetchPopularBooks() {
        // 베스트셀러(Bestseller) API 호출
        String url = String.format("http://www.aladin.co.kr/ttb/api/ItemList.aspx?ttbkey=%s&QueryType=Bestseller&MaxResults=50&start=1&SearchTarget=Book&output=js&Version=20131101", 
                                   apiKey);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("item");

            for (Map<String, Object> bookMap : items) {
                String title = (String) bookMap.get("title");
                if (itemRepository.existsByTitle(title)) continue;

                // 1. Item이 아닌 StaticContent로 생성 (상속 구조 반영)
                StaticContent book = new StaticContent();
                book.setTitle(title);
                Number reviewRank = (Number) bookMap.get("customerReviewRank");
                if (reviewRank != null) {
                    book.setExternalRating(reviewRank.doubleValue());
                }
                book.setCategory(Category.BOOK);
                book.setGenre(parseBookGenre((String) bookMap.get("categoryName")));
                
                // 2. 설명 정제 (HTML 제거 및 150자 제한)
                String rawDesc = (String) bookMap.get("description");
                if (rawDesc != null) {
                    String cleanDesc = rawDesc.replaceAll("<[^>]*>", "").trim();
                    book.setDescription(cleanDesc.length() > 150 ? cleanDesc.substring(0, 147) + "..." : cleanDesc);
                }
                
                book.setImg((String) bookMap.get("cover"));

                // 3. StaticContent 전용 필드 채우기
                book.setCreator((String) bookMap.get("author"));    // 저자
                book.setPublisher((String) bookMap.get("publisher")); // 출판사
                book.setIsbn((String) bookMap.get("isbn13"));        // ISBN

                String pubDate = (String) bookMap.get("pubDate");
                if (pubDate != null && !pubDate.isEmpty()) {
                    book.setReleaseDate(LocalDate.parse(pubDate));
                }

                // 4. 저장 (staticContentRepository 사용)
                staticContentRepository.save(book);
            }
        } catch (Exception e) {
            System.err.println("알라딘 데이터 수집 중 오류: " + e.getMessage());
        }
    }

    private String parseBookGenre(String rawGenre) {
        if (rawGenre == null) return "기타";
        if (rawGenre.contains("판타지")) return "판타지";
        if (rawGenre.contains("추리") || rawGenre.contains("미스터리")) return "추리";
        if (rawGenre.contains("스릴러")) return "스릴러";
        if (rawGenre.contains("공포") || rawGenre.contains("호러")) return "공포";
        if (rawGenre.contains("SF")) return "SF";
        if (rawGenre.contains("로맨스")) return "로맨스";
        if (rawGenre.contains("소설")) return "문학"; 
        if (rawGenre.contains("에세이")) return "수필";
        if (rawGenre.contains("자기계발")) return "자기계발";
        if (rawGenre.contains("역사")) return "역사";

        String[] parts = rawGenre.split(">");
        return parts.length > 1 ? parts[1].trim() : "도서";
    }
}
