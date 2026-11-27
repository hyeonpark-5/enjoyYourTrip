package com.hyeon.enjoyyourtrip.domain.member.dto.Response;

import java.time.LocalDateTime;

import com.hyeon.enjoyyourtrip.domain.member.entity.Role;

public record MemberResponse(
        Long id,
        String email,
        String name,
        Role role,
        LocalDateTime createdAt) {
}
