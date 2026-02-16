package com.ddu.culture.config;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.service.AladinService;
import com.ddu.culture.service.SpotifyService;
import com.ddu.culture.service.TmdbService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Profile("init")
public class DummyDataLoader implements CommandLineRunner {

    private final ItemRepository itemRepository;
    private final TmdbService tmdbService;
    private final AladinService aladinService;
    private final SpotifyService spotifyService;

    @Override
    public void run(String... args) {
        // 1. [영상] 영화 데이터 수집 (Movie)
        long movieCount = itemRepository.countByCategory(Category.MOVIE);
        if (movieCount < 100) { 
            System.out.println("🎬 영화 데이터 부족 (" + movieCount + "/100). 수집 시작...");
            for (int i = 1; i <= 5; i++) {
                tmdbService.fetchPopularMovies(i);
            }
            System.out.println("✅ 영화 데이터 수집 완료!");
        } else {
            System.out.println("🎬 영화 데이터가 이미 충분합니다. (현재: " + movieCount + "개)");
        }

        // 2. [영상] TV 시리즈 데이터 수집 (드라마, 예능, 시즌제 애니메이션)
        // 드라마, 예능, 애니메이션 카테고리를 합산해서 체크합니다.
        long tvCount = itemRepository.countByCategory(Category.DRAMA) 
                     + itemRepository.countByCategory(Category.TV_SHOW)
                     + itemRepository.countByCategory(Category.ANIMATION);

        if (tvCount < 100) {
            System.out.println("📺 TV 시리즈(드라마/예능/시즌제 애니) 데이터 부족 (" + tvCount + "/100). 수집 시작...");
            for (int i = 1; i <= 5; i++) {
                // 이 메서드 안에서 장르에 따라 DRAMA, TV_SHOW, ANIMATION으로 자동 분류됩니다.
                tmdbService.fetchPopularTvShows(i); 
            }
            System.out.println("✅ TV 시리즈 데이터 수집 완료!");
        } else {
            System.out.println("📺 TV 시리즈 데이터가 이미 충분합니다. (현재: " + tvCount + "개)");
        }

        // 3. [도서] 책 데이터 수집 (BestSeller)
        long bookCount = itemRepository.countByCategory(Category.BOOK);
        if (bookCount < 30) {
            System.out.println("📚 책 데이터 부족 (" + bookCount + "/30). 수집 시작...");
            aladinService.fetchPopularBooks();
            System.out.println("✅ 책 데이터 수집 완료!");
        } else {
            System.out.println("📚 책 데이터가 이미 충분합니다. (현재: " + bookCount + "개)");
        }

        // 4. [음악] 음악 데이터 수집 (Gemini AI 장르 분석 포함)
        long musicCount = itemRepository.countByCategory(Category.MUSIC);
        if (musicCount < 50) {
            System.out.println("🎵 음악 데이터 부족 (" + musicCount + "/50). 수집 시작 (Gemini 가동)...");
            spotifyService.fetchPopularMusic();
            System.out.println("✅ 음악 데이터 수집 완료!");
        } else {
            System.out.println("🎵 음악 데이터가 이미 충분합니다. (현재: " + musicCount + "개)");
        }
    }
    
    // saveItem 메서드는 추상 클래스 Item을 직접 생성하므로 삭제하거나 주석 처리하는 것이 좋습니다.
}
