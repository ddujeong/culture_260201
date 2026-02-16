package com.ddu.culture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ddu.culture.entity.StaticContent;
import java.util.List;
import java.util.Optional;


@Repository
public interface StaticContentRepository extends JpaRepository<StaticContent, Long> {

	Optional<StaticContent> findBySpotifyTrackId(String spotifyTrackId);
}
