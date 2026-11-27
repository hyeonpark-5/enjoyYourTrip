package com.hyeon.enjoyyourtrip.domain.member.dto.Request;

import jakarta.validation.constraints.Pattern;

/**
 * 회원 목록 검색 조건 (쿼리 파라미터). key가 없거나 word가 비어 있으면 전체 조회.
 *
 * @param key  name | email
 * @param word 포함 검색어
 */
public record MemberSearchRequest(
        @Pattern(regexp = "name|email", message = "검색 항목은 name 또는 email 입니다.")
        String key,

        String word) {

    public boolean hasCondition() {
        return key != null && word != null && !word.isBlank();
    }
}
