package com.pozit.pozitserver.term.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 최종 약관 종류/필수 여부가 아직 확정되지 않아, termType을 고정된 enum이 아닌 자유 문자열로 받는다.
// 정책 확정 후 특정 코드를 필수로 강제해야 하면 TermAgreementService에 검증을 추가한다.
public record TermAgreementItemRequest(
        @Schema(description = "약관 종류 코드 (기획 확정 전이라 자유 문자열, 예: SERVICE, PRIVACY, LOCATION, AGE_OVER_14)", example = "SERVICE")
        @NotBlank(message = "약관 종류는 필수입니다.")
        @Size(max = 30, message = "약관 종류는 30자 이하여야 합니다.")
        String termType,

        @Schema(description = "동의 여부", example = "true")
        @NotNull(message = "동의 여부는 필수입니다.")
        Boolean agreed
) {}
