package com.hyeon.enjoyyourtrip.global.config;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.hyeon.enjoyyourtrip.global.security.CurrentMemberIdArgumentResolver;

import lombok.RequiredArgsConstructor;

/** @CurrentMemberId 파라미터 등록 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /** 웹 슬라이스 테스트처럼 회원 기능이 없는 환경에서도 뜨도록 지연 조회한다. */
    private final ObjectProvider<CurrentMemberIdArgumentResolver> currentMemberIdResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        currentMemberIdResolver.ifAvailable(resolvers::add);
    }
}
