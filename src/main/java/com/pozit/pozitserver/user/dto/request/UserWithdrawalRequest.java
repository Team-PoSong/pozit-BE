package com.pozit.pozitserver.user.dto.request;

import com.pozit.pozitserver.global.auth.ios.ApplePlatform;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 탈퇴 요청")
public record UserWithdrawalRequest(
        @Schema(
                description = "Apple 회원 탈퇴 시 필수입니다. 탈퇴 직전 앱에서 Apple 재인증을 수행하고 받은 authorizationCode를 전달합니다. 카카오 회원은 전달하지 않습니다.",
                example = "c1234567890..."
        )
        String appleAuthorizationCode,

        @Schema(
                description = "Apple 회원 탈퇴 시 필수입니다. authorizationCode를 발급받은 플랫폼입니다. IOS는 bundleId, ANDROID는 serviceId로 Apple 토큰 요청을 수행합니다.",
                example = "IOS"
        )
        ApplePlatform applePlatform
) {
}
