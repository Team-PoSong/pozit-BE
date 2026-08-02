package com.pozit.pozitserver.global.auth.dto.response;

import com.pozit.pozitserver.user.domain.User;

public record LoginTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshTokenExpiresIn,
        Long userId,
        String nickname,
        boolean isNewUser
) {

    public static LoginTokenResponse of(
            String accessToken,
            String refreshToken,
            long expiresIn,
            long refreshTokenExpiresIn,
            User user,
            boolean isNewUser
    ) {
        return new LoginTokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                refreshTokenExpiresIn,
                user.getId(),
                user.getNickname(),
                isNewUser
        );
    }
}
