package com.hyeon.enjoyyourtrip.domain.member.service;

import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.domain.member.entity.Role;
import com.hyeon.enjoyyourtrip.domain.member.repository.MemberRepository;
import com.hyeon.enjoyyourtrip.global.config.AdminProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서버 시작 시 .env의 ADMIN_EMAIL/ADMIN_PASSWORD로 관리자 계정을 만든다(이미 있으면 건너뜀).
 * 비밀번호가 SQL 파일(깃)에 남지 않도록 마이그레이션 대신 이 방식을 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
public class AdminInitializer implements ApplicationRunner {

    private final AdminProperties properties;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            return;
        }
        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (memberRepository.existsByEmail(email)) {
            return;
        }
        memberRepository.save(Member.create(
                email, passwordEncoder.encode(properties.password()), properties.name(), Role.ADMIN));
        log.info("관리자 계정을 생성했습니다: {}", email);
    }
}
