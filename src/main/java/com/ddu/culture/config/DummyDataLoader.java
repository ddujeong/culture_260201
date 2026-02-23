package com.ddu.culture.config;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
        syncAll(false);
    }

    public void syncAll(boolean force) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        System.out.println("🔄 [데이터 동기화 검사 시작]");

        // 1. 영화 & TV (TMDB)
        if (force || !hasUpdatedToday(Category.MOVIE, startOfToday)) {
            System.out.println("🎬 영화/TV 최신화 중...");
            tmdbService.fetchPopularMovies(1);
            tmdbService.fetchPopularTvShows(1);
            // 과거 데이터 보충도 영화 업데이트 시점에 같이 실행
            System.out.println("📺 한국 예능 데이터 수집 중...");
            tmdbService.fetchKoreanTvShows(1);      // 한국 인기 예능 (나혼산, 런닝맨 등)
            tmdbService.fetchKoreanTvShows(2);      // 좀 더 풍성하게 2페이지까지
            
         // ⭐ 애니메이션 추가 (영화/TV 애니메이션 모두 수집)
            System.out.println("⛩️ 인기 애니메이션 수집 중...");
            tmdbService.fetchPopularAnimations(1);
            
            expandPastMovieData();
        } else {
            System.out.println("✅ 영화/TV는 이미 최신 상태입니다.");
        }

        // 2. 도서 (Aladin)
        if (force || !hasUpdatedToday(Category.BOOK, startOfToday)) {
            System.out.println("📚 도서 최신화 중...");
            aladinService.fetchPopularBooks();
        } else {
            System.out.println("✅ 도서는 이미 최신 상태입니다.");
        }

        // 3. 음악 (Spotify)
        if (force || !hasUpdatedToday(Category.MUSIC, startOfToday)) {
            System.out.println("🎵 음악 최신화 중...");
            spotifyService.fetchPopularMusic();
        } else {
            System.out.println("✅ 음악은 이미 최신 상태입니다.");
        }

        System.out.println("✨ 모든 동기화 프로세스 종료");
    }

    // 특정 카테고리에 오늘 업데이트된 아이템이 있는지 체크
    private boolean hasUpdatedToday(Category category, LocalDateTime startOfToday) {
        return itemRepository.existsByCategoryAndUpdatedAtAfter(category, startOfToday);
    }

    private void expandPastMovieData() {
        int randomPage = (int)(Math.random() * 40) + 2;
        tmdbService.fetchPopularMovies(randomPage);
    }
}
