package com.hyeon.enjoyyourtrip.domain.post.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.domain.member.entity.Role;
import com.hyeon.enjoyyourtrip.domain.member.exception.MemberNotFoundException;
import com.hyeon.enjoyyourtrip.domain.member.repository.MemberRepository;
import com.hyeon.enjoyyourtrip.domain.post.dto.Request.PostCreateRequest;
import com.hyeon.enjoyyourtrip.domain.post.dto.Request.PostSearchRequest;
import com.hyeon.enjoyyourtrip.domain.post.dto.Request.PostUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.post.dto.Response.PostDetailResponse;
import com.hyeon.enjoyyourtrip.domain.post.dto.Response.PostSummaryResponse;
import com.hyeon.enjoyyourtrip.domain.post.entity.Post;
import com.hyeon.enjoyyourtrip.domain.post.entity.PostType;
import com.hyeon.enjoyyourtrip.domain.post.exception.PostForbiddenException;
import com.hyeon.enjoyyourtrip.domain.post.exception.PostNotFoundException;
import com.hyeon.enjoyyourtrip.domain.post.mapper.PostMapper;
import com.hyeon.enjoyyourtrip.domain.post.repository.PostRepository;
import com.hyeon.enjoyyourtrip.global.common.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * 게시판. 권한 규칙(원본과 동일)
 * - 목록·상세: 누구나
 * - 공지(NOTICE) 작성: 관리자 / 공유글(POST) 작성: 로그인 회원
 * - 수정·삭제: 작성자 또는 관리자
 * 권한은 토큰이 아닌 DB의 현재 회원 정보로 판단한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PostMapper postMapper;

    public PageResponse<PostSummaryResponse> search(PostSearchRequest condition, Pageable pageable) {
        PostType type = condition.type();
        Page<Post> page;
        if (!condition.hasCondition()) {
            page = postRepository.findByType(type, pageable);
        } else {
            String word = condition.word().trim();
            page = switch (condition.key()) {
                case "title" -> postRepository.findByTypeAndTitleContaining(type, word, pageable);
                case "content" -> postRepository.findByTypeAndContentContaining(type, word, pageable);
                default -> postRepository.findByTypeAndAuthorNameContaining(type, word, pageable);
            };
        }
        return PageResponse.of(page, postMapper::toSummary);
    }

    /** 상세 조회 시 조회수를 1 올린다. */
    @Transactional
    public PostDetailResponse read(Long id) {
        if (postRepository.increaseViewCount(id) == 0) {
            throw new PostNotFoundException(id);
        }
        return postMapper.toDetail(findWithAuthor(id));
    }

    @Transactional
    public PostDetailResponse write(PostCreateRequest request, Long writerId) {
        Member writer = findMember(writerId);
        if (request.type() == PostType.NOTICE && writer.getRole() != Role.ADMIN) {
            throw PostForbiddenException.noticeWrite();
        }
        Post post = postRepository.save(postMapper.toEntity(request, writer));
        return postMapper.toDetail(post);
    }

    @Transactional
    public PostDetailResponse edit(Long id, PostUpdateRequest request, Long editorId) {
        Post post = findWithAuthor(id);
        checkOwnerOrAdmin(post, editorId);
        post.edit(request.title().trim(), request.content().trim());
        postRepository.flush();
        return postMapper.toDetail(post);
    }

    @Transactional
    public void delete(Long id, Long editorId) {
        Post post = findWithAuthor(id);
        checkOwnerOrAdmin(post, editorId);
        postRepository.delete(post);
    }

    private void checkOwnerOrAdmin(Post post, Long memberId) {
        if (post.isWrittenBy(memberId)) {
            return;
        }
        if (findMember(memberId).getRole() != Role.ADMIN) {
            throw PostForbiddenException.notOwner();
        }
    }

    private Post findWithAuthor(Long id) {
        return postRepository.findWithAuthorById(id).orElseThrow(() -> new PostNotFoundException(id));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }
}
