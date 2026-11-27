package com.hyeon.enjoyyourtrip.domain.post.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 게시글 수정. 게시판 종류는 바꿀 수 없다. */
public record PostUpdateRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 80, message = "제목은 80자 이내로 입력해주세요.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        @Size(max = 2000, message = "내용은 2000자 이내로 입력해주세요.")
        String content) {
}
