package com.pozit.pozitserver.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_member_provider_social_id",
                        columnNames = {"provider", "social_id"}
                ),
                @UniqueConstraint(
                        name = "uk_member_nickname",
                        columnNames = {"nickname"}
                )
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;

    @Column(name = "social_id", nullable = false)
    private String socialId;

//    private String email;

    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    @Column(name = "noti_travel_enabled", nullable = false)
    private Boolean notiTravelEnabled = true;

    @Column(name = "noti_group_enabled", nullable = false)
    private Boolean notiGroupEnabled = true;

    @Column(name = "noti_pozing_enabled", nullable = false)
    private Boolean notiPozingEnabled = true;

    @Column(name = "noti_course_enabled", nullable = false)
    private Boolean notiCourseEnabled = true;

    @Column(name = "noti_notice_enabled", nullable = false)
    private Boolean notiNoticeEnabled = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private User(
            SocialProvider provider,
            String socialId,
//            String email,
            String nickname,
            Role role
    ) {
        this.provider = provider;
        this.socialId = socialId;
//        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }

    public void updateProfile(
//            String email,
            String nickname
    ) {
//        this.email = email;
        this.nickname = nickname;
    }

    public void updateNotificationSettings(
            Boolean pushEnabled,
            Boolean notiTravelEnabled,
            Boolean notiGroupEnabled,
            Boolean notiPozingEnabled,
            Boolean notiCourseEnabled,
            Boolean notiNoticeEnabled
    ) {
        if (pushEnabled != null) this.pushEnabled = pushEnabled;
        if (notiTravelEnabled != null) this.notiTravelEnabled = notiTravelEnabled;
        if (notiGroupEnabled != null) this.notiGroupEnabled = notiGroupEnabled;
        if (notiPozingEnabled != null) this.notiPozingEnabled = notiPozingEnabled;
        if (notiCourseEnabled != null) this.notiCourseEnabled = notiCourseEnabled;
        if (notiNoticeEnabled != null) this.notiNoticeEnabled = notiNoticeEnabled;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void withdraw() {
        String suffix = id + ":" + UUID.randomUUID();
        this.socialId = "deleted:" + suffix;
        this.nickname = "탈퇴한 사용자_" + id;
        this.deletedAt = LocalDateTime.now();
        this.pushEnabled = false;
        this.notiTravelEnabled = false;
        this.notiGroupEnabled = false;
        this.notiPozingEnabled = false;
        this.notiCourseEnabled = false;
        this.notiNoticeEnabled = false;
    }
}
