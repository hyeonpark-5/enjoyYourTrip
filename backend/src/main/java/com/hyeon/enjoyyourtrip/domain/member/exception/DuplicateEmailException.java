package com.hyeon.enjoyyourtrip.domain.member.exception;

import com.hyeon.enjoyyourtrip.global.exception.ConflictException;

public class DuplicateEmailException extends ConflictException {

    public DuplicateEmailException(String email) {
        super("DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다: " + email);
    }
}
