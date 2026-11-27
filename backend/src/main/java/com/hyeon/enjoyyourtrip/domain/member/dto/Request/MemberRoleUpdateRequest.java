package com.hyeon.enjoyyourtrip.domain.member.dto.Request;

import com.hyeon.enjoyyourtrip.domain.member.entity.Role;

import jakarta.validation.constraints.NotNull;

/** 권한 변경 (관리자 전용) */
public record MemberRoleUpdateRequest(
        @NotNull(message = "권한은 필수입니다.")
        Role role) {
}
