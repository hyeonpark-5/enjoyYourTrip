package com.hyeon.enjoyyourtrip.global.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * 목록 응답 공통 형식. Spring Data의 Page를 그대로 직렬화하면 내부 구조가 노출되고 버전마다 바뀌므로 감싼다.
 * page는 0부터 시작한다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> converter) {
        return new PageResponse<>(
                page.getContent().stream().map(converter).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
