package com.pozit.pozitserver.notification.repository;

import com.pozit.pozitserver.notification.domain.Notification;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.user.domain.User;
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

    long countByUserAndIsReadFalse(User user);

    @Modifying
    @Query("delete from Notification n where n.travel = :travel")
    void deleteByTravel(@Param("travel") Travel travel);

    @Modifying
    @Query(value = """
            delete from notifications
            where user_id = :userId
            and id not in (
                select id from notifications
                where user_id = :userId
                order by created_at desc
                limit :limit
            )
            """, nativeQuery = true)
    void deleteExcessByUser(@Param("userId") Long userId, @Param("limit") int limit);
}
