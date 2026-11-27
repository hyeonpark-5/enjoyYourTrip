package com.hyeon.enjoyyourtrip.domain.member.exception;

import com.hyeon.enjoyyourtrip.global.exception.NotFoundException;

public class MemberNotFoundException extends NotFoundException {

    public MemberNotFoundException(Long id) {
        super("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다. id=" + id);
    }
}
