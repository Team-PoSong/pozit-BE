package com.pozit.pozitserver.global.auth.ios;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "애플 네이티브 앱 로그인 요청")
public record AppleIdentityTokenRequest(
        @Schema(description = "Flutter Kakao SDK의 OAuthToken.accessToken 값")
        @NotBlank
        String identityToken,

        @NotBlank
        String authorizationCode,

        @NotNull
        ApplePlatform platform,

        String email,

        String givenName,

        String familyName
) {
}