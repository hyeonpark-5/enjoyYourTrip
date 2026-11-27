package com.hyeon.enjoyyourtrip.domain.post.dto.Response;

import java.time.LocalDateTime;

import com.hyeon.enjoyyourtrip.domain.post.entity.PostType;

/**
 * 게시글 상세. 프론트엔드는 authorId와 내 정보(/members/me)를 비교해 수정·삭제 버튼을 보여준다.
 * title/content는 사용자가 입력한 원문이므로 화면에 넣을 때 반드시 텍스트로 출력(escape)해야 한다.
 */
public record PostDetailResponse(
        Long id,
        PostType type,
        String title,
        String content,
        Long authorId,
        String authorName,
        int viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
