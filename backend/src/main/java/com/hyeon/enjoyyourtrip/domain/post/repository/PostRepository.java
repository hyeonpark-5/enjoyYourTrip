package com.hyeon.enjoyyourtrip.domain.post.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hyeon.enjoyyourtrip.domain.post.entity.Post;
import com.hyeon.enjoyyourtrip.domain.post.entity.PostType;

/**
 * 목록·상세는 작성자 이름을 함께 보여주므로 @EntityGraph로 author를 한 번에 가져온다(N+1 방지).
 * 대소문자 구분 없는 검색은 테이블 콜레이션(utf8mb4_0900_ai_ci)이 처리한다.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = "author")
    Optional<Post> findWithAuthorById(Long id);

    @EntityGraph(attributePaths = "author")
    Page<Post> findByType(PostType type, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Post> findByTypeAndTitleContaining(PostType type, String title, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Post> findByTypeAndContentContaining(PostType type, String content, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Post> findByTypeAndAuthorNameContaining(PostType type, String authorName, Pageable pageable);

    /** 동시에 여러 명이 읽어도 조회수가 유실되지 않도록 DB에서 바로 1 증가시킨다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    int increaseViewCount(@Param("id") Long id);
}
