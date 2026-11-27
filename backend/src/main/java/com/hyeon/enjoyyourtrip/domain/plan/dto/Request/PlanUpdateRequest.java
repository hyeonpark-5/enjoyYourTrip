package com.hyeon.enjoyyourtrip.domain.plan.dto.Request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 여행 계획 수정 (JSON, 전체 교체). 보낸 값으로 이름·경비·기간·메모·동선이 모두 바뀐다.
 */
public record PlanUpdateRequest(
        @NotBlank(message = "계획 이름을 입력해 주세요.")
        @Size(max = 40, message = "계획 이름은 40자 이내로 입력해 주세요.")
        String name,

        @Min(value = 0, message = "예상 경비는 0 이상이어야 합니다.")
        @Max(value = 10_000_000_000L, message = "예상 경비는 100억 원 이하로 입력해 주세요.")
        Long budget,

        @NotNull(message = "출발일을 선택해 주세요.")
        LocalDate startDate,

        @NotNull(message = "도착일을 선택해 주세요.")
        LocalDate endDate,

        @Size(max = 800, message = "상세 계획은 800자 이내로 입력해 주세요.")
        String notes,

        @NotEmpty(message = "여행 동선에 관광지를 한 곳 이상 추가해 주세요.")
        @Size(max = 50, message = "여행 동선은 50곳까지 추가할 수 있습니다.")
        List<@Valid @NotNull PlanStopRequest> stops) {
}
