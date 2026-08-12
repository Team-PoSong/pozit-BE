package com.pozit.pozitserver.notification.service;

import com.pozit.pozitserver.notification.domain.Notification;
import com.pozit.pozitserver.notification.domain.NotificationType;
import com.pozit.pozitserver.notification.dto.response.NotificationListResponse;
import com.pozit.pozitserver.notification.dto.response.NotificationUnreadCountResponse;
import com.pozit.pozitserver.notification.repository.NotificationRepository;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int MAX_NOTIFICATION_COUNT = 15;

    private final NotificationRepository notificationRepository;

    @Transactional
    public List<NotificationListResponse> getNotifications(User user) {
        List<NotificationListResponse> responses = notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(notification -> new NotificationListResponse(
                        notification.getId(),
                        notification.getType().name(),
                        notification.getTravel() != null ? notification.getTravel().getTitle() : null,
                        notification.getContent(),
                        notification.getIsRead(),
                        notification.getCreatedAt()
                ))
                .toList();

        notificationRepository.markAllAsRead(user);

        return responses;
    }

    // 읽음 처리 없는 순수 조회 (배지용)
    public NotificationUnreadCountResponse getUnreadCount(User user) {
        return new NotificationUnreadCountResponse(notificationRepository.countByUserAndIsReadFalse(user));
    }

    @Transactional
    public void createNotification(User user, Travel travel, NotificationType type, String content) {
        Notification notification = Notification.builder()
                .user(user)
                .travel(travel)
                .type(type)
                .content(content)
                .build();
        notificationRepository.save(notification);

        long count = notificationRepository.countByUser(user);
        if (count > MAX_NOTIFICATION_COUNT) {
            long excess = count - MAX_NOTIFICATION_COUNT;
            List<Notification> oldestOnes = notificationRepository
                    .findByUserOrderByCreatedAtAsc(user, PageRequest.of(0, (int) excess));
            notificationRepository.deleteAll(oldestOnes);
        }
    }
}
