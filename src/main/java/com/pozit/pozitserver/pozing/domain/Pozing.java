package com.pozit.pozitserver.pozing.domain;

import com.pozit.pozitserver.course.domain.CourseSpot;
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
@Table(name = "pozings")
public class Pozing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_spot_id", nullable = false)
    private CourseSpot courseSpot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "pozing_url", nullable = false)
    private String pozingUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    private Pozing(CourseSpot courseSpot, User user, String pozingUrl, String thumbnailUrl) {
        this.courseSpot = courseSpot;
        this.user = user;
        this.pozingUrl = pozingUrl;
        this.thumbnailUrl = thumbnailUrl;
    }
}
