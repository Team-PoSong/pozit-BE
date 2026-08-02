package com.pozit.pozitserver.global.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "카카오 네이티브 앱 로그인 요청")
public record KakaoAccessTokenRequest(
        @Schema(description = "Flutter Kakao SDK의 OAuthToken.accessToken 값", example = "kakao_access_token")
        @NotBlank
        String accessToken,

        @Schema(
                description = "카카오에서 발급하는 값이 아닙니다. 앱 클라이언트가 최초 실행 또는 설치 시 생성한 UUID를 secure storage 등에 저장하고, 같은 기기의 로그인/재발급/로그아웃 요청에서 동일하게 전달해야 합니다.",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @NotBlank
        String deviceId
) {
}
