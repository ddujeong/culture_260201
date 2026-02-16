package com.ddu.culture.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.VideoContent;
import com.ddu.culture.repository.ItemRepository;
import com.ddu.culture.repository.VideoContentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TmdbService {

    @Value("${tmdb.api.key}")
    private String apiKey;

    private final VideoContentRepository videoContentRepository; 
    private final ItemRepository itemRepository; 
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void fetchPopularMovies(int page) {
        String url = String.format("https://api.themoviedb.org/3/movie/popular?api_key=%s&language=ko-KR&page=%d", 
                apiKey.trim(), page);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

            for (Map<String, Object> movie : results) {
                String title = (String) movie.get("title");
                // ⚠️ 중요: 상세 정보를 위해 id 추출이 필요합니다.
                Long tmdbId = ((Number) movie.get("id")).longValue();

                if (itemRepository.existsByTitle(title)) continue;

                VideoContent video = new VideoContent(); 
                video.setTitle(title);
                video.setCategory(Category.MOVIE);
                Number voteAverage = (Number) movie.get("vote_average");
                if (voteAverage != null) {
                    video.setExternalRating(voteAverage.doubleValue());
                }
                List<Integer> genreIds = (List<Integer>) movie.get("genre_ids");
                if (genreIds != null && genreIds.contains(16)) {
                    video.setCategory(Category.ANIMATION);
                }

                video.setGenre(mapTmdbGenre(genreIds));
                video.setDescription((String) movie.get("overview"));
                
                String releaseDate = (String) movie.get("release_date");
                if (releaseDate != null && !releaseDate.isEmpty()) {
                    video.setReleaseDate(LocalDate.parse(releaseDate));
                }
                
                video.setImg("https://image.tmdb.org/t/p/w500" + movie.get("poster_path"));

                // 상세 정보(감독/출연진/OTT) 채우기 호출
                updateVideoDetails(video, tmdbId, "movie"); 
                
                videoContentRepository.save(video);
            }
        } catch (Exception e) {
            System.err.println("TMDB 영화 데이터 수집 중 오류 발생: " + e.getMessage());
        }
    }

    @Transactional
    public void fetchPopularTvShows(int page) {
        String url = String.format("https://api.themoviedb.org/3/tv/popular?api_key=%s&language=ko-KR&page=%d", 
                apiKey.trim(), page);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

            for (Map<String, Object> tv : results) {
                String name = (String) tv.get("name");
                Long tmdbId = ((Number) tv.get("id")).longValue();

                if (itemRepository.existsByTitle(name)) continue;

                VideoContent video = new VideoContent();
                video.setTitle(name);
                Number voteAverage = (Number) tv.get("vote_average");
                if (voteAverage != null) {
                    video.setExternalRating(voteAverage.doubleValue());
                }
                List<Integer> genreIds = (List<Integer>) tv.get("genre_ids");
                video.setCategory(determineTvCategory(genreIds));
                video.setGenre(mapTmdbGenre(genreIds));
                video.setDescription((String) tv.get("overview"));
                
                String airDate = (String) tv.get("first_air_date");
                if (airDate != null && !airDate.isEmpty()) {
                    video.setReleaseDate(LocalDate.parse(airDate));
                }
                
                video.setImg("https://image.tmdb.org/t/p/w500" + tv.get("poster_path"));
                video.setOriginCountry(((List<String>) tv.get("origin_country")).stream().findFirst().orElse("KR"));

                // ✅ 수정: "movie"가 아니라 "tv"로 호출해야 합니다.
                updateVideoDetails(video, tmdbId, "tv"); 
                
                videoContentRepository.save(video);
            }
        } catch (Exception e) {
            System.err.println("TMDB TV 데이터 수집 중 오류 발생: " + e.getMessage());
        }
    }

    @Transactional
    public void updateVideoDetails(VideoContent video, Long tmdbId, String type) {
        String url = String.format(
            "https://api.themoviedb.org/3/%s/%d?api_key=%s&language=ko-KR&append_to_response=credits,watch/providers",
            type, tmdbId, apiKey.trim()
        );

        try {
            Map<String, Object> details = restTemplate.getForObject(url, Map.class);

            // 1. 출연진(Cast) 추출
            Map<String, Object> credits = (Map<String, Object>) details.get("credits");
            if (credits != null) {
                List<Map<String, Object>> castList = (List<Map<String, Object>>) credits.get("cast");
                String castNames = castList.stream()
                        .limit(5)
                        .map(c -> (String) c.get("name"))
                        .collect(Collectors.joining(", "));
                video.setCast(castNames);

                // 2. 감독(Director / Created By) 추출
                if ("movie".equals(type)) {
                    List<Map<String, Object>> crewList = (List<Map<String, Object>>) credits.get("crew");
                    String director = crewList.stream()
                            .filter(c -> "Director".equals(c.get("job")))
                            .map(c -> (String) c.get("name"))
                            .findFirst().orElse("Unknown");
                    video.setDirector(director);
                    video.setRuntime((Integer) details.get("runtime"));
                } else {
                    List<Map<String, Object>> createdBy = (List<Map<String, Object>>) details.get("created_by");
                    if (createdBy != null && !createdBy.isEmpty()) {
                        video.setDirector((String) createdBy.get(0).get("name"));
                    }
                    video.setTotalSeasons((Integer) details.get("number_of_seasons"));
                    video.setTotalEpisodes((Integer) details.get("number_of_episodes"));
                }
            }

            // 3. OTT 정보(Watch Providers) 추출
            Map<String, Object> watchProviders = (Map<String, Object>) details.get("watch/providers");
            if (watchProviders != null) {
                Map<String, Object> results = (Map<String, Object>) watchProviders.get("results");
                Map<String, Object> koProviders = (Map<String, Object>) results.get("KR");
                
                if (koProviders != null && koProviders.containsKey("flatrate")) {
                    List<Map<String, Object>> flatrate = (List<Map<String, Object>>) koProviders.get("flatrate");
                    String providers = flatrate.stream()
                            .map(p -> (String) p.get("provider_name"))
                            .collect(Collectors.joining(", "));
                    video.setOttProviders(providers);
                }
            }
        } catch (Exception e) {
            System.err.println(tmdbId + " 상세 정보 수집 실패: " + e.getMessage());
        }
    }

    private Category determineTvCategory(List<Integer> genreIds) {
        if (genreIds == null) return Category.DRAMA;
        if (genreIds.contains(16)) return Category.ANIMATION;
        if (genreIds.contains(10764) || genreIds.contains(10767)) return Category.TV_SHOW;
        return Category.DRAMA;
    }

    private String mapTmdbGenre(List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) return "기타";
        return switch (genreIds.get(0)) {
            case 28, 10759 -> "액션/어드벤처";
            case 16 -> "애니메이션";
            case 35 -> "코미디";
            case 80 -> "범죄";
            case 18 -> "드라마";
            case 10751 -> "가족";
            case 14, 10765 -> "판타지/SF";
            case 9648 -> "미스터리";
            case 10749 -> "로맨스";
            default -> "기타";
        };
    }
}
