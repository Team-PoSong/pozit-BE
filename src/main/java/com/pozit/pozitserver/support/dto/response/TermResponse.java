package com.pozit.pozitserver.support.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "약관 정보")
public record TermResponse(
        @Schema(description = "약관 제목", example = "서비스 이용약관")
        String title,

        @Schema(description = "약관 버전", example = "1.0")
        String version,

        @Schema(description = "시행일", example = "2026-07-01")
        LocalDate effectiveDate,

        @Schema(description = "약관 조항 목록")
        List<TermSectionResponse> sections
) {}
