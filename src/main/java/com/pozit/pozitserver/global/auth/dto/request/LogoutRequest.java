package com.pozit.pozitserver.global.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그아웃 요청")
public record LogoutRequest(
        @Schema(
                description = "로그아웃할 기기의 앱 생성 UUID입니다. 해당 userId/deviceId 조합의 refresh token만 폐기합니다.",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @NotBlank
        String deviceId
) {
}
