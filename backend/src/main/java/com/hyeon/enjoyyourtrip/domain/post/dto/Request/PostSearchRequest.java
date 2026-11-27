package com.hyeon.enjoyyourtrip.domain.post.dto.Request;

import com.hyeon.enjoyyourtrip.domain.post.entity.PostType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 게시글 목록 검색 조건 (쿼리 파라미터).
 *
 * @param type NOTICE | POST (필수)
 * @param key  title | content | author — 없거나 word가 비면 전체
 * @param word 포함 검색어
 */
public record PostSearchRequest(
        @NotNull(message = "게시판 종류(type)는 필수입니다.")
        PostType type,

        @Pattern(regexp = "title|content|author", message = "검색 항목은 title, content, author 중 하나입니다.")
        String key,

        String word) {

    public boolean hasCondition() {
        return key != null && word != null && !word.isBlank();
    }
}
