package com.pozit.pozitserver.notification.dto.response;

import java.time.LocalDateTime;

public record NotificationListResponse(
        Long notificationId,
        String type,
        String title,
        String content,
        Boolean isRead,
        LocalDateTime createdAt
) {}
