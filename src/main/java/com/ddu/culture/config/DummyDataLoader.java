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
        if (itemRepository.count() == 0) {

            // ===== 영화 20개 =====
            saveItem("스타워즈", Category.MOVIE, "SF", "우주 모험", LocalDate.of(1977,5,25));
            saveItem("인셉션", Category.MOVIE, "액션", "꿈 속 세계 탐험", LocalDate.of(2010,7,16));
            saveItem("라라랜드", Category.MOVIE, "뮤지컬", "꿈과 사랑 이야기", LocalDate.of(2016,12,7));
            saveItem("어벤져스", Category.MOVIE, "액션", "슈퍼히어로 팀업", LocalDate.of(2012,4,25));
            saveItem("토이 스토리", Category.MOVIE, "애니메이션", "장난감들의 모험", LocalDate.of(1995,11,22));
            saveItem("타이타닉", Category.MOVIE, "로맨스", "거대한 사랑 이야기", LocalDate.of(1997,12,19));
            saveItem("기생충", Category.MOVIE, "드라마", "사회 계층 이야기", LocalDate.of(2019,5,30));
            saveItem("인터스텔라", Category.MOVIE, "SF", "우주 탐사와 시간 여행", LocalDate.of(2014,11,7));
            saveItem("매트릭스", Category.MOVIE, "액션", "가상현실 속 혁명", LocalDate.of(1999,3,31));
            saveItem("조커", Category.MOVIE, "드라마", "혼돈과 사회적 소외", LocalDate.of(2019,10,4));
            saveItem("어벤져스: 엔드게임", Category.MOVIE, "액션", "마지막 결전", LocalDate.of(2019,4,24));
            saveItem("겨울왕국", Category.MOVIE, "애니메이션", "자매 이야기", LocalDate.of(2013,11,27));
            saveItem("해리포터와 마법사의 돌", Category.MOVIE, "판타지", "마법 모험", LocalDate.of(2001,11,16));
            saveItem("라이언 킹", Category.MOVIE, "애니메이션", "사자왕 이야기", LocalDate.of(1994,6,15));
            saveItem("라푼젤", Category.MOVIE, "애니메이션", "긴 머리 공주", LocalDate.of(2010,11,24));
            saveItem("블랙 팬서", Category.MOVIE, "액션", "와칸다 왕국 이야기", LocalDate.of(2018,2,14));
            saveItem("덩케르크", Category.MOVIE, "전쟁", "제2차 세계대전 구조 작전", LocalDate.of(2017,7,21));
            saveItem("센과 치히로의 행방불명", Category.MOVIE, "애니메이션", "환상적인 모험", LocalDate.of(2001,7,20));
            saveItem("인사이드 아웃", Category.MOVIE, "애니메이션", "감정의 성장 이야기", LocalDate.of(2015,6,19));
            saveItem("어바웃 타임", Category.MOVIE, "로맨스", "시간 여행 로맨스", LocalDate.of(2013,5,3));

            // ===== 책 20권 =====
            saveItem("해리포터", Category.BOOK, "판타지", "마법 학교 이야기", LocalDate.of(1997,6,26));
            saveItem("반지의 제왕", Category.BOOK, "판타지", "중간계 모험 이야기", LocalDate.of(1954,7,29));
            saveItem("어린 왕자", Category.BOOK, "동화", "철학적인 이야기", LocalDate.of(1943,4,6));
            saveItem("데미안", Category.BOOK, "문학", "성장과 자아 발견", LocalDate.of(1919,1,1));
            saveItem("총, 균, 쇠", Category.BOOK, "역사", "문명 발달 분석", LocalDate.of(1997,3,1));
            saveItem("호밀밭의 파수꾼", Category.BOOK, "문학", "청소년 성장 이야기", LocalDate.of(1951,7,16));
            saveItem("1984", Category.BOOK, "디스토피아", "전체주의 사회", LocalDate.of(1949,6,8));
            saveItem("동물농장", Category.BOOK, "풍자", "정치 풍자 이야기", LocalDate.of(1945,8,17));
            saveItem("노르웨이의 숲", Category.BOOK, "문학", "청춘과 사랑 이야기", LocalDate.of(1987,9,4));
            saveItem("백설공주에게 죽음을", Category.BOOK, "스릴러", "미스터리 사건", LocalDate.of(2005,1,10));
            saveItem("셜록 홈즈", Category.BOOK, "추리", "명탐정 이야기", LocalDate.of(1892,10,14));
            saveItem("앵무새 죽이기", Category.BOOK, "문학", "인종과 성장 이야기", LocalDate.of(1960,7,11));
            saveItem("그리스인 조르바", Category.BOOK, "문학", "인생과 자유 이야기", LocalDate.of(1946,2,1));
            saveItem("위대한 개츠비", Category.BOOK, "문학", "1920년대 미국 이야기", LocalDate.of(1925,4,10));
            saveItem("셜리", Category.BOOK, "문학", "19세기 여성 이야기", LocalDate.of(1849,1,1));
            saveItem("소공녀", Category.BOOK, "문학", "소녀 성장 이야기", LocalDate.of(1905,9,1));
            saveItem("미드나잇 라이브러리", Category.BOOK, "판타지", "선택과 삶 이야기", LocalDate.of(2020,9,29));
            saveItem("해변의 카프카", Category.BOOK, "문학", "자아 찾기", LocalDate.of(2002,1,15));
            saveItem("노인과 바다", Category.BOOK, "문학", "인생과 자연 이야기", LocalDate.of(1952,9,1));
            saveItem("연금술사", Category.BOOK, "판타지", "꿈과 운명 이야기", LocalDate.of(1988,4,1));

            // ===== 음악 20곡 =====
            saveItem("Shape of You", Category.MUSIC, "Pop", "에드 시런의 대표곡", LocalDate.of(2017,1,6));
            saveItem("Billie Jean", Category.MUSIC, "Pop", "마이클 잭슨의 명곡", LocalDate.of(1983,1,2));
            saveItem("Bohemian Rhapsody", Category.MUSIC, "Rock", "퀸의 대표곡", LocalDate.of(1975,10,31));
            saveItem("Stairway to Heaven", Category.MUSIC, "Rock", "레드 제플린 명곡", LocalDate.of(1971,11,8));
            saveItem("Yesterday", Category.MUSIC, "Pop", "비틀즈 명곡", LocalDate.of(1965,8,26));
            saveItem("Rolling in the Deep", Category.MUSIC, "Pop", "아델 명곡", LocalDate.of(2010,11,29));
            saveItem("Smells Like Teen Spirit", Category.MUSIC, "Rock", "너바나 대표곡", LocalDate.of(1991,9,10));
            saveItem("Someone Like You", Category.MUSIC, "Pop", "아델 발라드", LocalDate.of(2011,1,24));
            saveItem("Hotel California", Category.MUSIC, "Rock", "이글스 명곡", LocalDate.of(1976,12,8));
            saveItem("Hey Jude", Category.MUSIC, "Pop", "비틀즈 명곡", LocalDate.of(1968,8,26));
            saveItem("Thriller", Category.MUSIC, "Pop", "마이클 잭슨 대표곡", LocalDate.of(1982,11,30));
            saveItem("Like a Rolling Stone", Category.MUSIC, "Rock", "밥 딜런 명곡", LocalDate.of(1965,7,20));
            saveItem("Imagine", Category.MUSIC, "Pop", "존 레논 명곡", LocalDate.of(1971,10,11));
            saveItem("Billie Eilish - Bad Guy", Category.MUSIC, "Pop", "빌리 아일리시 히트곡", LocalDate.of(2019,3,29));
            saveItem("Uptown Funk", Category.MUSIC, "Funk", "마크 론슨 & 브루노 마스", LocalDate.of(2014,11,10));
            saveItem("Shape of My Heart", Category.MUSIC, "Pop", "스팅 명곡", LocalDate.of(1993,9,21));
            saveItem("Numb", Category.MUSIC, "Rock", "링킨파크 대표곡", LocalDate.of(2003,3,25));
            saveItem("Happy", Category.MUSIC, "Pop", "퍼렐 윌리엄스 명곡", LocalDate.of(2013,11,21));
            saveItem("Rolling in the Deep (Live)", Category.MUSIC, "Pop", "아델 라이브 버전", LocalDate.of(2011,6,14));

            System.out.println("더미 아이템 60개 삽입 완료!");
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
