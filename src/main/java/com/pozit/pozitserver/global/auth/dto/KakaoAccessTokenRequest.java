package com.pozit.pozitserver.global.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoAccessTokenRequest(
        @NotBlank
        String accessToken
) {
}
