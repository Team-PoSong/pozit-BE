package com.pozit.pozitserver.global.auth.apple.jwt;

public record AppleTokenClaims (
        String socialId,
        String email,
        boolean emailVerified,
        boolean privateEmail
){
}
