package com.hyeon.enjoyyourtrip.domain.post.dto.Request;

import com.hyeon.enjoyyourtrip.domain.post.entity.PostType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotNull(message = "게시판 종류는 필수입니다.")
        PostType type,

        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 80, message = "제목은 80자 이내로 입력해주세요.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        @Size(max = 2000, message = "내용은 2000자 이내로 입력해주세요.")
        String content) {
}
