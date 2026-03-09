package com.ddu.culture.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("STATIC")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class StaticContent extends Item {
    
    private String creator;       // 저자 또는 아티스트
    private String publisher;     // 출판사 또는 레이블 (음악)
    
    // 도서 전용
    private String isbn;
    private Integer pageCount;
    
}