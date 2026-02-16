package com.ddu.culture.service;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.StaticContent;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.repository.StaticContentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SpotifyService {

    @Value("${spotify.api.key}")
    private String clientId;

    @Value("${spotify.secret.key}")
    private String clientSecret;

    private final StaticContentRepository staticContentRepository; // StaticContent 전용 사용
    private final ItemRepository itemRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final GeminiService geminiService;

    /* =========================
       1. Access Token 발급
       ========================= */
    private String getAccessToken() {
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encodedAuth);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response =
                restTemplate.postForEntity("https://accounts.spotify.com/api/token", request, Map.class);

        return (String) response.getBody().get("access_token");
    }

    /* =========================
       2. 인기 음악 수집
       ========================= */
    @Transactional
    public void fetchPopularMusic() {
        String token = getAccessToken();

        // 1. 수집하고 싶은 다양한 검색어들 (임영웅 독점을 피하기 위한 전략)
        String[] queries = {
            "year:2024",     // 최신곡
            "genre:k-pop",   // 아이돌/댄스
            "genre:pop",     // 팝송
            "genre:hip-hop", // 힙합
            "genre:indie"    // 인디
        };

        for (String query : queries) {
            try {
                // 공식 문서 스펙: limit은 최대 10까지만 가능
                URI uri = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/search")
                        .queryParam("q", query)
                        .queryParam("type", "track")
                        .queryParam("market", "KR")
                        .queryParam("limit", 10) 
                        .build()
                        .toUri();

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                
                ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

                Map<String, Object> body = response.getBody();
                if (body == null || !body.containsKey("tracks")) continue;

                Map<String, Object> tracksObj = (Map<String, Object>) body.get("tracks");
                List<Map<String, Object>> trackItems = (List<Map<String, Object>>) tracksObj.get("items");

                List<String> requestList = new ArrayList<>();
                List<Map<String, Object>> targetTracks = new ArrayList<>();
                
                for (Map<String, Object> track : trackItems) {
                    String title = (String) track.get("name");
                    List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");
                    String artistName = (String) artists.get(0).get("name");
                    
                    // 특수문자만 있는 제목 필터링
                    if (title.matches("^[0-9\\s\\W]+$")) continue;
                    
                    String fullKey = title + " (" + artistName + ")";
                    
                    // 중복 저장 방지 (이미 DB에 있으면 Gemini 분석 대상에서 제외)
                    if (itemRepository.existsByTitle(title + " - " + artistName)) continue;

                    requestList.add(fullKey);
                    targetTracks.add(track);
                }

                // 2. 수집된 곡이 있을 때만 Gemini 가동
                if (!requestList.isEmpty()) {
                    System.out.println("🚀 [" + query + "] 키워드로 " + requestList.size() + "곡 분석 시작...");
                    Map<String, String> genreResults = geminiService.inferGenresBulk(requestList);

                    for (Map<String, Object> track : targetTracks) {
                        String title = (String) track.get("name");
                        String artistName = (String) ((List<Map<String, Object>>) track.get("artists")).get(0).get("name");
                        String fullKey = title + " (" + artistName + ")";
                        String genre = genreResults.getOrDefault(fullKey, "Pop");
                        
                        saveStaticMusic(track, title, artistName, genre);
                    }
                }
                
                // API 쿼터와 과부하 방지를 위해 살짝 대기
                Thread.sleep(500);

            } catch (Exception e) {
                System.err.println("❌ [" + query + "] 수집 중 에러: " + e.getMessage());
            }
        }
        System.out.println("✅ 모든 장르의 음악 데이터 수집 및 상속 구조 저장 완료!");
    }

    private void saveStaticMusic(Map<String, Object> track, String title, String artistName, String genre) {
        String spotifyId = (String) track.get("id");
        String fullTitle = title + " - " + artistName;
        
        // 중복 체크: 제목 혹은 스포티파이 ID로 체크
        if (itemRepository.existsByTitle(fullTitle)) return;

        Map<String, Object> album = (Map<String, Object>) track.get("album");
        List<Map<String, Object>> images = (List<Map<String, Object>>) album.get("images");
        String imageUrl = (images != null && !images.isEmpty()) ? (String) images.get(0).get("url") : "";
        String albumName = (String) album.get("name");

        // 1. StaticContent 생성
        StaticContent music = new StaticContent();
        music.setTitle(fullTitle);
        music.setCategory(Category.MUSIC);
        music.setGenre(genre);
        music.setImg(imageUrl);
        music.setDescription(artistName + "의 [" + albumName + "] 앨범 수록곡입니다.");

        // 2. 음악 특화 필드 채우기
        music.setCreator(artistName);      // 아티스트를 creator에 저장
        music.setAlbumName(albumName);     // 앨범명
        music.setSpotifyTrackId(spotifyId); // 중복 방지용 고유 ID

        staticContentRepository.save(music);
    }
}
