package com.pozit.pozitserver.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_member_provider_social_id",
                        columnNames = {"provider", "social_id"}
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
}
