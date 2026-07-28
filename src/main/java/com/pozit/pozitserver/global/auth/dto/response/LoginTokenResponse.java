package com.pozit.pozitserver.global.auth.dto.response;

import com.pozit.pozitserver.user.domain.User;

public record LoginTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String nickname,
        boolean isNewUser
) {

    public static LoginTokenResponse of(
            String accessToken,
            long expiresIn,
            User user,
            boolean isNewUser
    ) {
        return new LoginTokenResponse(
                accessToken,
                "Bearer",
                expiresIn,
                user.getId(),
                user.getNickname(),
                isNewUser
        );
    }
}
