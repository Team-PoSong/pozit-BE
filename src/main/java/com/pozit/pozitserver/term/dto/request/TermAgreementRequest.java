package com.pozit.pozitserver.term.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "약관 동의 요청. 약관 종류 및 필수 여부는 기획 확정 전이며, 전달된 항목의 동의 여부를 저장한다.")
public record TermAgreementRequest(
        @Schema(description = "약관 종류별 동의 목록")
        @NotEmpty(message = "약관 동의 목록은 필수입니다.")
        @Valid
        List<TermAgreementItemRequest> agreements
) {}
