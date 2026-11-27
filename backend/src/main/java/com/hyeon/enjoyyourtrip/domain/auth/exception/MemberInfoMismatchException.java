package com.hyeon.enjoyyourtrip.domain.auth.exception;

import com.hyeon.enjoyyourtrip.global.exception.InvalidValueException;

/** 비밀번호 재설정 시 이메일과 이름이 일치하는 회원이 없음 */
public class MemberInfoMismatchException extends InvalidValueException {

    public MemberInfoMismatchException() {
        super("MEMBER_INFO_MISMATCH", "이메일과 이름이 일치하는 회원이 없습니다.");
    }
}
