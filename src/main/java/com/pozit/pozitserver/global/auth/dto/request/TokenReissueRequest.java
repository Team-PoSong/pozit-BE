package com.pozit.pozitserver.global.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급 요청")
public record TokenReissueRequest(
        @Schema(description = "Refresh Token", example = "pozit_refresh_token")
        @NotBlank
        String refreshToken,

        @Schema(
                description = "로그인 시 refresh token을 발급받을 때 전달했던 앱 생성 UUID입니다. Redis에 저장된 기기별 refresh token을 찾는 데 사용되므로 같은 기기에서는 동일한 값을 전달해야 합니다.",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @NotBlank
        String deviceId
) {
}
