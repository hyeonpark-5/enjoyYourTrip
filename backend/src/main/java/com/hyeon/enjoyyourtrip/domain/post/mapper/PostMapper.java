package com.hyeon.enjoyyourtrip.domain.post.mapper;

import org.springframework.stereotype.Component;

import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.domain.post.dto.Request.PostCreateRequest;
import com.hyeon.enjoyyourtrip.domain.post.dto.Response.PostDetailResponse;
import com.hyeon.enjoyyourtrip.domain.post.dto.Response.PostSummaryResponse;
import com.hyeon.enjoyyourtrip.domain.post.entity.Post;

@Component
public class PostMapper {

    static final String WITHDRAWN_AUTHOR = "탈퇴한 회원";

    public Post toEntity(PostCreateRequest request, Member author) {
        return Post.create(request.type(), request.title().trim(), request.content().trim(), author);
    }

    public PostSummaryResponse toSummary(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getType(),
                post.getTitle(),
                authorName(post.getAuthor()),
                post.getViewCount(),
                post.getCreatedAt());
    }

    public PostDetailResponse toDetail(Post post) {
        return new PostDetailResponse(
                post.getId(),
                post.getType(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getId(),
                authorName(post.getAuthor()),
                post.getViewCount(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    private static String authorName(Member author) {
        return author.isWithdrawn() ? WITHDRAWN_AUTHOR : author.getName();
    }
}
