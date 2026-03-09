package com.ddu.culture.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ddu.culture.entity.Actor;
import com.ddu.culture.entity.Category;
import com.ddu.culture.entity.Director;
import com.ddu.culture.entity.Item;
import com.ddu.culture.entity.Season;
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

                if (videoContentRepository.existsByTmdbId(tmdbId)) {
                    continue;
                }

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
                video.setTmdbId(tmdbId);
                video.setGenre(mapTmdbGenre(genreIds));
                video.setDescription((String) movie.get("overview"));
                Number popularity = (Number) movie.get("popularity");
                if (popularity != null) {
                    video.setPopularity(popularity.doubleValue());
                }

                Number voteCount = (Number) movie.get("vote_count");
                if (voteCount != null) {
                    video.setVoteCount(voteCount.intValue());
                }
                String releaseDate = (String) movie.get("release_date");
                if (releaseDate != null && !releaseDate.isEmpty()) {
                    video.setReleaseDate(LocalDate.parse(releaseDate));
                }
                String poster = (String) movie.get("poster_path");

                if (poster != null) {
                    video.setImg("https://image.tmdb.org/t/p/w500" + poster);
                }
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

                if (videoContentRepository.existsByTmdbId(tmdbId)) {
                    continue;
                }

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
                video.setTmdbId(tmdbId);
                String airDate = (String) tv.get("first_air_date");
                if (airDate != null && !airDate.isEmpty()) {
                    video.setReleaseDate(LocalDate.parse(airDate));
                }
                Number popularity = (Number) tv.get("popularity");
                if (popularity != null) {
                    video.setPopularity(popularity.doubleValue());
                }

                Number voteCount = (Number) tv.get("vote_count");
                if (voteCount != null) {
                    video.setVoteCount(voteCount.intValue());
                }
                String poster = (String) tv.get("poster_path");
                if (poster != null) {
                    video.setImg("https://image.tmdb.org/t/p/w500" + poster);
                }
                List<String> countries = (List<String>) tv.get("origin_country");

                if (countries != null && !countries.isEmpty()) {
                    video.setOriginCountry(countries.get(0));
                } else {
                    video.setOriginCountry("KR");
                }
                updateVideoDetails(video, tmdbId, "tv"); 
                videoContentRepository.save(video);
                
            }
        } catch (Exception e) {
            System.err.println("TMDB TV 데이터 수집 중 오류 발생: " + e.getMessage());
        }
    }
    @Transactional
    public void fetchKoreanTvShows(int page) {
        // 💡 discover API를 사용해서 '한국(KR)' + '예능(10764, 10767)'만 필터링
        String url = String.format(
            "https://api.themoviedb.org/3/discover/tv?api_key=%s&language=ko-KR&page=%d" +
            "&with_genres=10764,10767&with_origin_country=KR&sort_by=popularity.desc",
            apiKey.trim(), page
        );

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

            for (Map<String, Object> tv : results) {
                String name = (String) tv.get("name");
                Long tmdbId = ((Number) tv.get("id")).longValue();

                if (videoContentRepository.existsByTmdbId(tmdbId)) {
                    continue;
                }

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
                video.setTmdbId(tmdbId);
                String airDate = (String) tv.get("first_air_date");
                if (airDate != null && !airDate.isEmpty()) {
                    video.setReleaseDate(LocalDate.parse(airDate));
                }
                String poster = (String) tv.get("poster_path");
                if (poster != null) {
                    video.setImg("https://image.tmdb.org/t/p/w500" + poster);
                }
                Number popularity = (Number) tv.get("popularity");
                if (popularity != null) {
                    video.setPopularity(popularity.doubleValue());
                }

                Number voteCount = (Number) tv.get("vote_count");
                if (voteCount != null) {
                    video.setVoteCount(voteCount.intValue());
                }
                List<String> countries = (List<String>) tv.get("origin_country");

                if (countries != null && !countries.isEmpty()) {
                    video.setOriginCountry(countries.get(0));
                } else {
                    video.setOriginCountry("KR");
                }   
                updateVideoDetails(video, tmdbId, "tv");
                videoContentRepository.save(video);
                
            }
        } catch (Exception e) {
            System.err.println("한국 예능 수집 중 에러: " + e.getMessage());
        }
    }
    @Transactional
    public void fetchPopularAnimations(int page) {
        // 💡 영화 중에서 애니메이션(장르 16)만 인기순으로 가져오기
        String movieUrl = String.format(
            "https://api.themoviedb.org/3/discover/movie?api_key=%s&language=ko-KR&page=%d" +
            "&with_genres=16&sort_by=popularity.desc",
            apiKey.trim(), page
        );

        // 💡 TV 시리즈 중에서 애니메이션(장르 16)만 인기순으로 가져오기
        String tvUrl = String.format(
            "https://api.themoviedb.org/3/discover/tv?api_key=%s&language=ko-KR&page=%d" +
            "&with_genres=16&sort_by=popularity.desc",
            apiKey.trim(), page
        );

        // 수집 로직 실행 (이미 만들어두신 fetchPopularMovies나 fetchPopularTvShows의 내부 로직과 유사하게 처리)
        fetchAndSaveFromUrl(movieUrl, "movie");
        fetchAndSaveFromUrl(tvUrl, "tv");
    }

    // 공통 로직 처리를 위한 private 메서드 (기존 코드 구조에 맞춰 적절히 구현)
    private void fetchAndSaveFromUrl(String url, String type) {
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            
            for (Map<String, Object> data : results) {
            	String title = "movie".equals(type) ? (String) data.get("title") : (String) data.get("name");
                Long tmdbId = ((Number) data.get("id")).longValue();

                if (videoContentRepository.existsByTmdbId(tmdbId)) {
                    continue;
                }

                VideoContent video = new VideoContent();
                video.setTitle(title);
                Number voteAverage = (Number) data.get("vote_average");
                if (voteAverage != null) {
                    video.setExternalRating(voteAverage.doubleValue());
                }
                List<Integer> genreIds = (List<Integer>) data.get("genre_ids");
                video.setCategory(Category.ANIMATION);
                video.setGenre(mapTmdbGenre(genreIds));
                video.setDescription((String) data.get("overview"));
                video.setTmdbId(tmdbId);
                String dateKey = "movie".equals(type) ? "release_date" : "first_air_date";
                String dateStr = (String) data.get(dateKey);
                if (dateStr != null && !dateStr.isEmpty()) {
                    video.setReleaseDate(LocalDate.parse(dateStr));
                }
                String poster = (String) data.get("poster_path");
                if (poster != null) {
                    video.setImg("https://image.tmdb.org/t/p/w500" + poster);
                }
                if (data.containsKey("origin_country")) {
                    List<String> countries = (List<String>) data.get("origin_country");
                    if (countries != null && !countries.isEmpty()) {
                        video.setOriginCountry(countries.get(0));
                    } else {
                        video.setOriginCountry("Unknown");
                    }
                }
                Number popularity = (Number) data.get("popularity");
                if (popularity != null) {
                    video.setPopularity(popularity.doubleValue());
                }

                Number voteCount = (Number) data.get("vote_count");
                if (voteCount != null) {
                    video.setVoteCount(voteCount.intValue());
                }
                updateVideoDetails(video, tmdbId, type);
                videoContentRepository.save(video);
               

            }
        } catch (Exception e) {
            System.err.println("애니메이션 수집 중 오류: " + e.getMessage());
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
                if (castList != null) {
                    video.getActors().clear(); // 기존 데이터 초기화
                    castList.stream().limit(8).forEach(c -> {
                        String name = (String) c.get("name");
                        String pPath = (String) c.get("profile_path");

                        Actor actor = new Actor();
                        String cleanName = sanitizeName(name);

                        if (cleanName != null) {
                            actor.setName(cleanName);
                            video.getActors().add(actor);
                        }
                        actor.setProfilePath(pPath != null ? "https://image.tmdb.org/t/p/w185" + pPath : null);
                        actor.setVideoContent(video);
                    });
                }
                video.getDirectors().clear();
                // 2. 감독(Director / Created By) 추출
                if ("movie".equals(type)) {
                	List<Map<String, Object>> crewList = (List<Map<String, Object>>) credits.get("crew");

                	if (crewList != null) {
                	    crewList.stream()
                	        .filter(c -> "Director".equals(c.get("job")))
                	        .limit(2)
                	        .forEach(c -> {
                	            Director director = new Director();
                	            String name = (String) c.get("name");
                	            String pPath = (String) c.get("profile_path");

                	            director.setName(name);
                	            director.setProfilePath(pPath != null ? "https://image.tmdb.org/t/p/w185" + pPath : null);
                	            director.setVideoContent(video);
                	            video.getDirectors().add(director);
                	        });
                	}
                } else {
                    List<Map<String, Object>> createdBy = (List<Map<String, Object>>) details.get("created_by");
                   if (createdBy != null) {
                        createdBy.forEach(c -> {
                            Director director = new Director();
                            director.setName((String) c.get("name"));
                            String pPath = (String) c.get("profile_path");
                            director.setProfilePath(pPath != null ? "https://image.tmdb.org/t/p/w185" + pPath : null);
                            director.setVideoContent(video);
                            video.getDirectors().add(director);
                        });
                    }
                    
                    video.setTotalSeasons((Integer) details.get("number_of_seasons"));
                    video.setTotalEpisodes((Integer) details.get("number_of_episodes"));
                }
            }

            // 3. OTT 정보(Watch Providers) 추출
            Map<String, Object> watchProviders = (Map<String, Object>) details.get("watch/providers");

            if (watchProviders != null) {
                Map<String, Object> results = (Map<String, Object>) watchProviders.get("results");

                if (results != null) {
                    Map<String, Object> koProviders = (Map<String, Object>) results.get("KR");

                    if (koProviders != null) {

                        List<Map<String, Object>> flatrate =
                            (List<Map<String, Object>>) koProviders.get("flatrate");

                        if (flatrate != null && !flatrate.isEmpty()) {

                            String cleanProviders = flatrate.stream()
                            .map(p -> (String) p.get("provider_name"))
                            .map(name -> {
                                // 핵심 브랜드명만 남기고 정제
                                if (name.contains("Netflix")) return "Netflix";
                                if (name.contains("Disney")) return "Disney+";
                                if (name.contains("Apple TV")) return "Apple TV+";
                                if (name.contains("Watcha")) return "왓챠";
                                if (name.contains("Wavve")) return "웨이브";
                                if (name.contains("TVING")) return "티빙"; // 👈 티빙 추가
                                if (name.contains("Coupang")) return "쿠팡플레이";
                                if (name.contains("Amazon Prime")) return "Amazon Prime Video";
                                if (name.contains("Naver")) return "네이버 시리즈온";
                                return name;
                            })
                            .distinct() // 중복 제거
                            .collect(Collectors.joining(", "));
                    video.setOttProviders(cleanProviders);
                }
                }
            }
            }
            if ("tv".equals(type)) {
                List<Map<String, Object>> seasonsData = (List<Map<String, Object>>) details.get("seasons");
                if (seasonsData != null) {
                    // 기존 시즌 데이터가 있다면 교체하기 위해 비움 (선택 사항)
                    video.getSeasons().clear();

                    for (Map<String, Object> s : seasonsData) {
                        // 'Special' 시즌(0번)을 제외하고 싶다면 아래 조건 추가
                        // if ((Integer) s.get("season_number") == 0) continue;
                    if (Integer.valueOf(0).equals(s.get("season_number"))) continue;
                        Season season = new Season();
                        season.setSeasonNumber((Integer) s.get("season_number"));
                        season.setName((String) s.get("name"));
                        season.setOverview((String) s.get("overview")); // 상세 줄거리
                        season.setEpisodeCount((Integer) s.get("episode_count"));
                        season.setAirDate((String) s.get("air_date"));
                        
                        String pPath = (String) s.get("poster_path");
                        if (pPath != null) {
                            season.setPosterPath("https://image.tmdb.org/t/p/w300" + pPath);
                        }
                        
                        season.setVideoContent(video);
                        video.getSeasons().add(season);
                    }
                }  
            }
        } catch (Exception e) {
            System.err.println(tmdbId + " 상세 정보 수집 실패: " + e.getMessage());
        }
    }

    private Category determineTvCategory(List<Integer> genreIds) {
        if (genreIds == null) return Category.DRAMA;
        if (genreIds.contains(16)) return Category.ANIMATION;
        if (genreIds.contains(10764) || genreIds.contains(10767)|| genreIds.contains(10763)) return Category.TV_SHOW;
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
 // 출연진/감독 이름 정제 유틸리티 메소드
    private String sanitizeName(String name) {
        if (name == null) return "Unknown";
        
        // 한글이 포함되어 있다면 그대로 사용
        if (name.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*")) {
            return name;
        }
        
        // 한글이 없고 한자가 포함되어 있다면? (중국어 이름 등)
        if (name.matches(".*[\\u4e00-\\u9fa5].*")) {
            // 이 경우, TMDB에서 해당 인물의 영문 이름을 다시 가져와야 하지만, 
            // 간단하게는 "알 수 없음" 처리하거나 한자만 제거할 수 있습니다.
            return null; 
        }
        
        // 영어 이름은 그대로 유지
        return name;
    }
}
