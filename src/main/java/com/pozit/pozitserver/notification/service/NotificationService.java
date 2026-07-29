package com.pozit.pozitserver.notification.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.notification.domain.Notification;
import com.pozit.pozitserver.notification.domain.NotificationType;
import com.pozit.pozitserver.notification.dto.response.NotificationListResponse;
import com.pozit.pozitserver.notification.repository.NotificationRepository;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationListResponse> getNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(notification -> new NotificationListResponse(
                        notification.getId(),
                        notification.getType().name(),
                        notification.getContent(),
                        notification.getIsRead(),
                        notification.getCreatedAt()
                ))
                .toList();
    }

}
