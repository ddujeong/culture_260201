package com.ddu.culture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ddu.culture.entity.VideoContent;

@Repository
public interface VideoContentRepository extends JpaRepository<VideoContent, Long> {
}