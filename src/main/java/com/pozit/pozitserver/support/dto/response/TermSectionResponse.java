package com.pozit.pozitserver.support.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약관 조항")
public record TermSectionResponse(
        @Schema(description = "조항 제목", example = "제1조 (목적)")
        String title,

        @Schema(description = "조항 내용", example = "본 약관은...")
        String content
) {}
