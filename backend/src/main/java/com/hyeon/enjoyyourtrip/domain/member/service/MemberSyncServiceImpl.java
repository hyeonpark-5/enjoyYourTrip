package com.hyeon.enjoyyourtrip.domain.member.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.domain.member.exception.MemberWithdrawnException;
import com.hyeon.enjoyyourtrip.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberSyncServiceImpl implements MemberSyncService {

    private static final int MAX_NAME = 50;

    private final MemberRepository memberRepository;

    @Override
    public Long sync(LoginUser user) {
        Member member = memberRepository.findByKeycloakId(user.keycloakId())
                .orElseGet(() -> linkOrCreate(user));
        if (member.isWithdrawn()) {
            throw new MemberWithdrawnException();
        }
        // 역할 원본은 Keycloak. 관리 콘솔에서 바꾼 역할도 다음 토큰부터 반영된다.
        if (member.getRole() != user.role()) {
            member.changeRole(user.role());
        }
        return member.getId();
    }

    private Member linkOrCreate(LoginUser user) {
        String email = user.email().trim().toLowerCase(Locale.ROOT);
        return memberRepository.findByEmailAndDeletedAtIsNull(email)
                .map(existing -> {
                    existing.linkKeycloak(user.keycloakId());
                    return existing;
                })
                .orElseGet(() -> {
                    if (memberRepository.existsByEmail(email)) {
                        throw new MemberWithdrawnException();
                    }
                    return memberRepository.save(Member.create(user.keycloakId(), email, displayName(user), user.role()));
                });
    }

    private static String displayName(LoginUser user) {
        String name = user.name() == null || user.name().isBlank() ? user.email() : user.name().trim();
        return name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name;
    }
}
