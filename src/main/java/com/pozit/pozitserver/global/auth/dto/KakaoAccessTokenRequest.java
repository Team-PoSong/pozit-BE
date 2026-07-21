package com.pozit.pozitserver.global.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "카카오 네이티브 앱 로그인 요청")
public record KakaoAccessTokenRequest(
        @Schema(description = "Flutter Kakao SDK의 OAuthToken.accessToken 값", example = "kakao_access_token")
        @NotBlank
        String accessToken
) {
}
