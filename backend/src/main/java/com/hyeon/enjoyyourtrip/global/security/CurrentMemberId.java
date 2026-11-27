package com.hyeon.enjoyyourtrip.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 로그인 회원 id(Long)를 넣는다. 로그인이 필요한 API에만 쓴다.
 *
 * <pre>
 * public PostDetailResponse write(@RequestBody PostCreateRequest request, @CurrentMemberId Long memberId)
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentMemberId {
}
