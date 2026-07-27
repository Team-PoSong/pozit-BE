package com.pozit.pozitserver.global.auth.ios;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "애플 네이티브 앱 로그인 요청")
public record AppleIdentityTokenRequest(
        @Schema(description = "Apple 로그인 후 발급받은 identityToken", example = "eyJraWQiOiJ...")
        @NotBlank
        String identityToken,

        @Schema(description = "Apple 로그인 후 발급받은 authorizationCode", example = "c1234567890...")
        @NotBlank
        String authorizationCode,

        @Schema(description = "Apple 로그인 요청 시 사용한 nonce 값. 클라이언트가 Apple에 전달한 nonce와 동일한 값이어야 합니다.", example = "d8f3b2a1c9")
        @NotBlank
        String nonce,

        @Schema(description = "Apple 로그인 요청 플랫폼. IOS는 bundleId, ANDROID는 serviceId로 audience를 검증합니다.", example = "IOS")
        @NotNull
        ApplePlatform platform,

        @Schema(description = "Apple 최초 로그인 시 전달받은 이메일. identityToken에 이메일이 없을 때 참고용으로 사용할 수 있습니다.", example = "user@example.com")
        String email,

        @Schema(description = "Apple 최초 로그인 시 전달받은 이름", example = "Minseo")
        String givenName,

        @Schema(description = "Apple 최초 로그인 시 전달받은 성", example = "Kim")
        String familyName
) {
}
