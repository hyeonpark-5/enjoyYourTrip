package com.hyeon.enjoyyourtrip.domain.post.service;

import org.springframework.data.domain.Pageable;

import com.hyeon.enjoyyourtrip.domain.post.dto.Request.PostCreateRequest;
import com.hyeon.enjoyyourtrip.domain.post.dto.Request.PostSearchRequest;
import com.hyeon.enjoyyourtrip.domain.post.dto.Request.PostUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.post.dto.Response.PostDetailResponse;
import com.hyeon.enjoyyourtrip.domain.post.dto.Response.PostSummaryResponse;
import com.hyeon.enjoyyourtrip.global.common.PageResponse;

/**
 * 게시판. 권한 규칙(원본과 동일)
 * - 목록·상세: 누구나
 * - 공지(NOTICE) 작성: 관리자 / 공유글(POST) 작성: 로그인 회원
 * - 수정·삭제: 작성자 또는 관리자
 */
public interface PostService {

    PageResponse<PostSummaryResponse> search(PostSearchRequest condition, Pageable pageable);

    /** 상세 조회 시 조회수를 1 올린다. */
    PostDetailResponse read(Long id);

    PostDetailResponse write(PostCreateRequest request, Long writerId);

    PostDetailResponse edit(Long id, PostUpdateRequest request, Long editorId);

    void delete(Long id, Long editorId);
}
