package com.hyeon.enjoyyourtrip.domain.member.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원 정보 수정. 이메일은 바꿀 수 없다.
 *
 * @param password 바꿀 때만 보낸다(null이면 유지)
 */
public record MemberUpdateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
        String name,

        @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해주세요.")
        String password) {
}
