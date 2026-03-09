package com.ddu.culture.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ddu.culture.entity.Category;
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
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void fetchPopularBooks() {
        // 1. 최신 베스트셀러 1페이지 (무조건 동기화)
        fetchFromAladin("Bestseller", 1);
        
        // 2. 과거 데이터 보충을 위해 랜덤하게 다른 페이지 수집 (2~10페이지 중 하나)
        int randomPage = (int) (Math.random() * 9) + 2;
        System.out.println("📚 [과거 도서 보충] 베스트셀러 " + randomPage + "페이지 수집 시작...");
        fetchFromAladin("Bestseller", randomPage);
    }

    private void fetchFromAladin(String queryType, int startPage) {
        String url = String.format(
            "http://www.aladin.co.kr/ttb/api/ItemList.aspx?ttbkey=%s&QueryType=%s&MaxResults=50&start=%d&SearchTarget=Book&output=js&Version=20131101",
            apiKey, queryType, startPage
        );

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !response.containsKey("item")) return;

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("item");

            for (Map<String, Object> bookMap : items) {
                String isbn = (String) bookMap.get("isbn13");
                if (isbn == null || isbn.isEmpty()) continue;

                // 🌟 ISBN 기반 Upsert 로직
                StaticContent book = staticContentRepository.findByIsbn(isbn)
                        .orElse(new StaticContent());

                book.setTitle((String) bookMap.get("title"));
                book.setIsbn(isbn);
                book.setCategory(Category.BOOK);
                book.setImg((String) bookMap.get("cover"));
                book.setCreator((String) bookMap.get("author"));
                book.setPublisher((String) bookMap.get("publisher"));
                book.setGenre(parseBookGenre((String) bookMap.get("categoryName")));

                // 평점 업데이트
                Number reviewRank = (Number) bookMap.get("customerReviewRank");
                if (reviewRank != null) {
                    book.setExternalRating(reviewRank.doubleValue());
                }

                // 설명 정제
                String rawDesc = (String) bookMap.get("description");
                if (rawDesc != null) {
                    String cleanDesc = rawDesc.replaceAll("<[^>]*>", "").trim();
                    book.setDescription(cleanDesc.length() > 150 ? cleanDesc.substring(0, 147) + "..." : cleanDesc);
                }

                String pubDate = (String) bookMap.get("pubDate");
                if (pubDate != null && !pubDate.isEmpty()) {
                    book.setReleaseDate(LocalDate.parse(pubDate));
                }

                staticContentRepository.save(book);
            }
        } catch (Exception e) {
            System.err.println("❌ 알라딘 [" + queryType + " P." + startPage + "] 수집 중 오류: " + e.getMessage());
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
