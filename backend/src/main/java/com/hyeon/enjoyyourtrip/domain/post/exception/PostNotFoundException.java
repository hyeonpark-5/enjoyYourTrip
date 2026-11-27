package com.hyeon.enjoyyourtrip.domain.post.exception;

import com.hyeon.enjoyyourtrip.global.exception.NotFoundException;

public class PostNotFoundException extends NotFoundException {

    public PostNotFoundException(Long id) {
        super("POST_NOT_FOUND", "게시글을 찾을 수 없습니다. id=" + id);
    }
}
