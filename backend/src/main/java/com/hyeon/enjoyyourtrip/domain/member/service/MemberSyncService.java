package com.hyeon.enjoyyourtrip.domain.member.service;

/**
 * Keycloak 사용자를 이 서비스의 회원(members)과 맞춘다.
 * 회원 정보(이름, 글·핫플레이스·계획의 주인)는 우리 DB에, 로그인·비밀번호·역할 원본은 Keycloak에 있다.
 */
public interface MemberSyncService {

    /**
     * 로그인 사용자에 해당하는 회원 id를 돌려준다. 처음 보는 사용자면 회원을 만들고,
     * Keycloak 도입 전 같은 이메일로 가입한 회원이 있으면 그 회원과 잇는다. 역할은 토큰 값으로 맞춘다.
     *
     * @throws com.hyeon.enjoyyourtrip.domain.member.exception.MemberWithdrawnException 탈퇴한 회원
     */
    Long sync(LoginUser user);
}
