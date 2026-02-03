package com.ddu.culture.config;

import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.repository.ItemRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class DummyDataLoader implements CommandLineRunner {

    private final ItemRepository itemRepository;

    @Override
    public void run(String... args) {
        if (itemRepository.count() == 0) { // DB에 아이템 없으면 실행
            
            // ===== 영화 더미 =====
            saveItem("스타워즈", Category.MOVIE, "SF", "우주 모험", LocalDate.of(1977, 5, 25));
            saveItem("인셉션", Category.MOVIE, "액션", "꿈 속 세계를 탐험하는 이야기", LocalDate.of(2010, 7, 16));
            saveItem("라라랜드", Category.MOVIE, "뮤지컬", "꿈과 사랑의 이야기", LocalDate.of(2016, 12, 7));
            saveItem("어벤져스", Category.MOVIE, "액션", "슈퍼히어로 팀업", LocalDate.of(2012, 4, 25));
            saveItem("토이 스토리", Category.MOVIE, "애니메이션", "장난감들의 모험", LocalDate.of(1995, 11, 22));

            // ===== 책 더미 =====
            saveItem("해리포터", Category.BOOK, "판타지", "마법 학교 이야기", LocalDate.of(1997, 6, 26));
            saveItem("반지의 제왕", Category.BOOK, "판타지", "중간계 모험 이야기", LocalDate.of(1954, 7, 29));
            saveItem("어린 왕자", Category.BOOK, "동화", "철학적인 이야기", LocalDate.of(1943, 4, 6));
            saveItem("데미안", Category.BOOK, "문학", "성장과 자아 발견", LocalDate.of(1919, 1, 1));
            saveItem("총, 균, 쇠", Category.BOOK, "역사", "문명 발달 분석", LocalDate.of(1997, 3, 1));

            // ===== 음악 더미 =====
            saveItem("Shape of You", Category.MUSIC, "Pop", "에드 시런의 대표곡", LocalDate.of(2017, 1, 6));
            saveItem("Billie Jean", Category.MUSIC, "Pop", "마이클 잭슨의 명곡", LocalDate.of(1983, 1, 2));
            saveItem("Bohemian Rhapsody", Category.MUSIC, "Rock", "퀸의 대표곡", LocalDate.of(1975, 10, 31));
            saveItem("Stairway to Heaven", Category.MUSIC, "Rock", "레드 제플린 명곡", LocalDate.of(1971, 11, 8));
            saveItem("Yesterday", Category.MUSIC, "Pop", "비틀즈 명곡", LocalDate.of(1965, 8, 26));

            System.out.println("더미 아이템 삽입 완료!");
        }
    }

    private void saveItem(String title, Category category, String genre, String description, LocalDate releaseDate) {
        Item item = new Item();
        item.setTitle(title);
        item.setCategory(category);
        item.setGenre(genre);
        item.setDescription(description);
        item.setReleaseDate(releaseDate);
        itemRepository.save(item);
    }
}
