package com.pozit.pozitserver.notification.repository;

import com.pozit.pozitserver.notification.domain.Notification;
import com.pozit.pozitserver.user.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            select n
            from Notification n
            left join fetch n.travel
            where n.user = :user
            order by n.createdAt desc
            """)
    List<Notification> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    List<Notification> findByUserOrderByCreatedAtAsc(User user, Pageable pageable);

    long countByUser(User user);

    long countByUserAndIsReadFalse(User user);

    @Modifying
    @Query("""
            update Notification n
            set n.isRead = true
            where n.user = :user and n.isRead = false
            """)
    void markAllAsRead(@Param("user") User user);
}
