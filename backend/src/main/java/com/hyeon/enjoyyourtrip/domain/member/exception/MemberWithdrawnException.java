package com.hyeon.enjoyyourtrip.domain.member.exception;

import com.hyeon.enjoyyourtrip.global.exception.ForbiddenException;

/** 탈퇴한 회원의 토큰이거나, 탈퇴한 이메일로 다시 가입한 경우 */
public class MemberWithdrawnException extends ForbiddenException {

    public MemberWithdrawnException() {
        super("MEMBER_WITHDRAWN", "탈퇴한 회원입니다. 같은 이메일로는 다시 이용할 수 없습니다.");
    }
}
