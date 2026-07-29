package com.pozit.pozitserver.user.dto.response;

public record UserInfoResponse(
        Long userId,
        String nickname,
        String socialProvider,
//        String email
        Boolean pushEnabled,
        Boolean notiTravelEnabled,
        Boolean notiGroupEnabled,
        Boolean notiPozingEnabled,
        Boolean notiCourseEnabled,
        Boolean notiNoticeEnabled
) {}
