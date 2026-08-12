package com.pozit.pozitserver.term.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "약관 동의 요청. 약관 종류는 기획 확정 전이라 자유 문자열이며, SERVICE/PRIVACY/LOCATION/AGE_OVER_14는 현재 필수로 agreed=true여야 한다.")
public record TermAgreementRequest(
        @Schema(description = "약관 종류별 동의 목록")
        @NotEmpty(message = "약관 동의 목록은 필수입니다.")
        List<@NotNull @Valid TermAgreementItemRequest> agreements
) {}
