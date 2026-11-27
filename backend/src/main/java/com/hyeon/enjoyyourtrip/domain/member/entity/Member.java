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

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt로 인코딩된 비밀번호 */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** 탈퇴 시각. null이면 활동 중인 회원 */
    private LocalDateTime deletedAt;

    private Member(String email, String encodedPassword, String name, Role role) {
        this.email = Objects.requireNonNull(email);
        this.password = Objects.requireNonNull(encodedPassword);
        this.name = Objects.requireNonNull(name);
        this.role = Objects.requireNonNull(role);
    }

    public static Member create(String email, String encodedPassword, String name, Role role) {
        return new Member(email, encodedPassword, name, role);
    }

    public void changeName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public void changePassword(String encodedPassword) {
        this.password = Objects.requireNonNull(encodedPassword);
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
