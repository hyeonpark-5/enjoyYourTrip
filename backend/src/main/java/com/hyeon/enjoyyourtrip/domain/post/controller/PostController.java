package com.hyeon.enjoyyourtrip.domain.post.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hyeon.enjoyyourtrip.domain.post.dto.Request.PostCreateRequest;
import com.hyeon.enjoyyourtrip.domain.post.dto.Request.PostSearchRequest;
import com.hyeon.enjoyyourtrip.domain.post.dto.Request.PostUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.post.dto.Response.PostDetailResponse;
import com.hyeon.enjoyyourtrip.domain.post.dto.Response.PostSummaryResponse;
import com.hyeon.enjoyyourtrip.domain.post.service.PostService;
import com.hyeon.enjoyyourtrip.global.common.PageResponse;
import com.hyeon.enjoyyourtrip.global.security.CurrentMemberId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Post", description = "게시판 (공지사항 · 공유게시판)")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 목록", description = "type=NOTICE|POST (필수), key=title|content|author, word, page=0부터, size")
    @GetMapping
    public PageResponse<PostSummaryResponse> search(
            @Valid @ModelAttribute PostSearchRequest condition,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return postService.search(condition, pageable);
    }

    @Operation(summary = "게시글 상세", description = "호출할 때마다 조회수가 1 오른다.")
    @GetMapping("/{id}")
    public PostDetailResponse read(@PathVariable Long id) {
        return postService.read(id);
    }

    @Operation(summary = "게시글 작성", description = "공지사항(NOTICE)은 관리자만 작성할 수 있다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailResponse write(@Valid @RequestBody PostCreateRequest request, @CurrentMemberId Long memberId) {
        return postService.write(request, memberId);
    }

    @Operation(summary = "게시글 수정 (작성자 또는 관리자)")
    @PatchMapping("/{id}")
    public PostDetailResponse edit(
            @PathVariable Long id, @Valid @RequestBody PostUpdateRequest request, @CurrentMemberId Long memberId) {
        return postService.edit(id, request, memberId);
    }

    @Operation(summary = "게시글 삭제 (작성자 또는 관리자)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentMemberId Long memberId) {
        postService.delete(id, memberId);
    }
}
