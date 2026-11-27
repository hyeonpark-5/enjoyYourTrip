package com.hyeon.enjoyyourtrip.domain.member.entity;

/** 회원 권한. JWT의 roles 클레임과 Spring Security 권한(ROLE_USER, ROLE_ADMIN)으로 쓰인다. */
public enum Role {
    USER,
    ADMIN
}
