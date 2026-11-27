package com.hyeon.enjoyyourtrip.domain.post.exception;

import com.hyeon.enjoyyourtrip.global.exception.ForbiddenException;

public class PostForbiddenException extends ForbiddenException {

    private PostForbiddenException(String message) {
        super("POST_FORBIDDEN", message);
    }

    public static PostForbiddenException noticeWrite() {
        return new PostForbiddenException("공지사항은 관리자만 작성할 수 있습니다.");
    }

    public static PostForbiddenException notOwner() {
        return new PostForbiddenException("작성자 또는 관리자만 수정·삭제할 수 있습니다.");
    }
}
