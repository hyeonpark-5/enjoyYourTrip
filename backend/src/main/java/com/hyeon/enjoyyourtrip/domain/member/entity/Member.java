package com.hyeon.enjoyyourtrip.domain.member.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hyeon.enjoyyourtrip.global.common.BaseTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Keycloak 사용자 id (토큰의 sub). Keycloak 도입 전 가입한 회원은 첫 로그인 때 채워진다. */
    @Column(unique = true, length = 36)
    private String keycloakId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;

    /** Keycloak realm 역할의 사본. 요청마다 토큰의 역할로 맞춘다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** 탈퇴 시각. null이면 활동 중인 회원 */
    private LocalDateTime deletedAt;

    private Member(String keycloakId, String email, String name, Role role) {
        this.keycloakId = Objects.requireNonNull(keycloakId);
        this.email = Objects.requireNonNull(email);
        this.name = Objects.requireNonNull(name);
        this.role = Objects.requireNonNull(role);
    }

    public static Member create(String keycloakId, String email, String name, Role role) {
        return new Member(keycloakId, email, name, role);
    }

    /** Keycloak 도입 전 가입한 회원을 Keycloak 사용자와 잇는다. */
    public void linkKeycloak(String keycloakId) {
        this.keycloakId = Objects.requireNonNull(keycloakId);
    }

    public void changeName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public void changeRole(Role role) {
        this.role = Objects.requireNonNull(role);
    }

    public void withdraw(LocalDateTime now) {
        this.deletedAt = Objects.requireNonNull(now);
    }

    public boolean isWithdrawn() {
        return deletedAt != null;
    }
}
