package com.pozit.pozitserver.global.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "구글 네이티브 앱 로그인 요청")
public record GoogleIdTokenRequest(
        @Schema(description = "Google 로그인 결과의 ID 토큰")
        @NotBlank String idToken
) {
}
