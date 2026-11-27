package com.hyeon.enjoyyourtrip.domain.auth.dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 (원본과 동일: 이메일과 이름이 일치하면 새 비밀번호로 변경).
 * 비밀번호 확인 입력 일치 여부는 프론트엔드에서 검사한다.
 */
public record PasswordResetRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해주세요.")
        String newPassword) {
}
