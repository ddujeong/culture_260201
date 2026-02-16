package com.ddu.culture.entity;

public enum Category {
    // VIDEO SECTION
    MOVIE("영상"), DRAMA("영상"), TV_SHOW("영상"), ANIMATION("영상"),
    
    // STATIC SECTION
    BOOK("텍스트"), MUSIC("음악");

    private final String sectionName;

    Category(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getSectionName() {
        return sectionName;
    }
}