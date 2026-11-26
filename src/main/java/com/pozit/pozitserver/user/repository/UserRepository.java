package com.pozit.pozitserver.user.repository;

import com.pozit.pozitserver.user.domain.SocialProvider;
import com.pozit.pozitserver.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndSocialId(
            SocialProvider provider,
            String socialId
    );

    @Modifying
    @Query(value = """
            insert into members (
                provider, social_id, nickname, role, status,
                push_enabled, noti_travel_enabled, noti_group_enabled,
                noti_pozing_enabled, noti_course_enabled, noti_notice_enabled
            )
            values ('GOOGLE', :socialId, :nickname, 'USER', 'ACTIVE',
                true, true, true, true, true, true)
            on conflict do nothing
            """, nativeQuery = true)
    int insertGoogleUserIfAbsent(@Param("socialId") String socialId, @Param("nickname") String nickname);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndIdNot(String nickname, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}
