package com.hyeon.enjoyyourtrip.global.exception;

import lombok.Getter;

/**
 * 모든 도메인 예외의 최상위 클래스. 웹(HTTP) 의존성이 없도록 상태 코드는 갖지 않고,
 * 아래 분류 클래스(NotFoundException 등)를 상속하는 것으로 의미를 나타낸다.
 * HTTP 상태 변환은 GlobalExceptionHandler가 맡는다.
 *
 * <pre>
 * // domain/member/exception
 * public class MemberNotFoundException extends NotFoundException {
 *     public MemberNotFoundException(long id) {
 *         super("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다. id=" + id);
 *     }
 * }
 * </pre>
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    /** 클라이언트가 분기 처리에 쓰는 고정 코드. 예: MEMBER_NOT_FOUND */
    private final String code;

    protected BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
