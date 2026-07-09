package com.pozit.pozitserver.user.dto.request;

public record NotificationSettingRequest(
        Boolean pushEnabled,
        Boolean notiTravelEnabled,
        Boolean notiGroupEnabled,
        Boolean notiPozingEnabled,
        Boolean notiCourseEnabled,
        Boolean notiNoticeEnabled
) {}
