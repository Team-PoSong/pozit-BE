package com.pozit.pozitserver.support.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "서비스 안내(약관/개인정보처리방침) 조회 응답")
public record SupportInfoResponse(
        @Schema(description = "이용약관")
        TermResponse serviceTerm,

        @Schema(description = "개인정보처리방침")
        TermResponse privacyPolicy
) {}
