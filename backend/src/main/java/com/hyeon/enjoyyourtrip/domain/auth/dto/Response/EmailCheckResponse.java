package com.hyeon.enjoyyourtrip.domain.auth.dto.Response;

/** 회원가입 전 이메일 중복 확인 결과 */
public record EmailCheckResponse(boolean available) {
}
