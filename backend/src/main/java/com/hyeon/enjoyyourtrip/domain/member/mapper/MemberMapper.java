package com.hyeon.enjoyyourtrip.domain.member.mapper;

import org.springframework.stereotype.Component;

import com.hyeon.enjoyyourtrip.domain.member.dto.Response.MemberResponse;
import com.hyeon.enjoyyourtrip.domain.member.entity.Member;

@Component
public class MemberMapper {

    public MemberResponse toResponse(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole(),
                member.getCreatedAt());
    }
}
