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
import org.springframework.web.client.HttpClientErrorException;
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

    private final StaticContentRepository staticContentRepository;
    private final ItemRepository itemRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final GeminiService geminiService;

    // 1. Access Token 발급 (기존과 동일)
    private String getAccessToken() {
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encodedAuth);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("https://accounts.spotify.com/api/token", request, Map.class);
        return (String) response.getBody().get("access_token");
    }

    // 2. 인기 음악 수집
    @Transactional
    public void fetchPopularMusic() {
        String token = getAccessToken();
        String[] queries = {"year:2026", "genre:k-pop", "genre:pop", "genre:hip-hop", "genre:indie"};

        for (String query : queries) {
            try {
                URI uri = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/search")
                        .queryParam("q", query)
                        .queryParam("type", "track")
                        .queryParam("market", "KR")
                        .queryParam("limit", 10) 
                        .build().toUri();

                
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

                Map<String, Object> body = response.getBody();
                if (body == null || !body.containsKey("tracks")) continue;

                List<Map<String, Object>> trackItems = (List<Map<String, Object>>) ((Map<String, Object>) body.get("tracks")).get("items");

                List<String> requestList = new ArrayList<>();
                List<Map<String, Object>> targetTracks = new ArrayList<>();

                for (Map<String, Object> track : trackItems) {
                    String title = (String) track.get("name");
                    String artistName = (String) ((List<Map<String, Object>>) track.get("artists")).get(0).get("name");
                    String spotifyId = (String) track.get("id");

                    if (title.matches("^[0-9\\s\\W]+$")) continue;

                    // ✨ [개선] 이미 있으면 업데이트만 하고, Gemini 분석 대상에서는 제외
                    if (staticContentRepository.existsBySpotifyTrackId(spotifyId)) {
                        updateMusicInfo(track, spotifyId); 
                        continue;
                    }

                    requestList.add(title + " (" + artistName + ")");
                    targetTracks.add(track);
                }

                // 새로운 곡들만 Gemini 가동
                if (!requestList.isEmpty()) {
                    System.out.println("🚀 [" + query + "] 신규 " + requestList.size() + "곡 분석 중...");
                    Map<String, String> genreResults = geminiService.inferGenresBulk(requestList);

                    for (Map<String, Object> track : targetTracks) {
                        String title = (String) track.get("name");
                        String artistName = (String) ((List<Map<String, Object>>) track.get("artists")).get(0).get("name");
                        String genre = genreResults.getOrDefault(title + " (" + artistName + ")", "Pop");
                        saveStaticMusic(track, title, artistName, genre);
                    }
                }
                Thread.sleep(1000);

            } catch (HttpClientErrorException.Forbidden e) {
                System.err.println("⚠️ [" + query + "] 스포티파이 정책상 접근이 일시적으로 제한되었습니다. (Skip)");
                // 여기서 중단하지 않고 continue를 하면 다음 장르 시도 가능
                continue; 
            } catch (Exception e) {
                System.err.println("❌ [" + query + "] 일반 에러: " + e.getMessage());
            }
        }
    }

    // ✨ [추가] 기존 데이터 업데이트 로직 (Upsert의 Update 파트)
    private void updateMusicInfo(Map<String, Object> track, String spotifyId) {
        staticContentRepository.findBySpotifyTrackId(spotifyId).ifPresent(music -> {
            Map<String, Object> album = (Map<String, Object>) track.get("album");
            List<Map<String, Object>> images = (List<Map<String, Object>>) album.get("images");
            if (images != null && !images.isEmpty()) {
                music.setImg((String) images.get(0).get("url")); // 이미지 최신화
            }
            // 필요한 경우 평점이나 인기순위 정보를 여기서 업데이트
            staticContentRepository.save(music);
        });
    }

    private void saveStaticMusic(Map<String, Object> track, String title, String artistName, String genre) {
        String spotifyId = (String) track.get("id");
        String fullTitle = title + " - " + artistName;

        Map<String, Object> album = (Map<String, Object>) track.get("album");
        String albumName = (String) album.get("name");
        List<Map<String, Object>> images = (List<Map<String, Object>>) album.get("images");
        String imageUrl = (images != null && !images.isEmpty()) ? (String) images.get(0).get("url") : "";

        StaticContent music = new StaticContent();
        music.setTitle(fullTitle);
        music.setCategory(Category.MUSIC);
        music.setGenre(genre);
        music.setImg(imageUrl);
        music.setDescription(artistName + "의 [" + albumName + "] 앨범 수록곡입니다.");
        music.setCreator(artistName);
        music.setAlbumName(albumName);
        music.setSpotifyTrackId(spotifyId);

        staticContentRepository.save(music);
    }
}