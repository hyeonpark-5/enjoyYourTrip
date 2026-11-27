package com.hyeon.enjoyyourtrip.domain.post.dto.Response;

import java.time.LocalDateTime;

import com.hyeon.enjoyyourtrip.domain.post.entity.PostType;

/** 목록 한 줄 (본문 제외) */
public record PostSummaryResponse(
        Long id,
        PostType type,
        String title,
        String authorName,
        int viewCount,
        LocalDateTime createdAt) {
}
