package com.pozit.pozitserver.user.dto.response;

public record UserInfoResponse(
        Long userId,
        String nickname,
        String socialProvider
//        String email
) {}
