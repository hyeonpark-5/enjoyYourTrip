package com.hyeon.enjoyyourtrip.domain.member.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원 정보 수정 (화면에 보이는 이름). 이메일·비밀번호는 Keycloak 계정 관리 화면에서 바꾼다.
 */
public record MemberUpdateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
        String name) {
}
