package com.pozit.pozitserver.travel.domain;

import com.pozit.pozitserver.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "travel_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_travel_member",
                        columnNames = {"travel_id", "user_id"}
                )
        }
)
public class TravelMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TravelMemberRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    private void prePersist() {
        this.joinedAt = LocalDateTime.now();
    }

    @Builder
    private TravelMember(Travel travel, User user, TravelMemberRole role) {
        this.travel = travel;
        this.user = user;
        this.role = role;
    }
}
