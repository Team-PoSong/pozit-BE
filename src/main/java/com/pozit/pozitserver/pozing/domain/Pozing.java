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

    @Column(name = "pozing_object_key", nullable = false)
    private String pozingObjectKey;

    @Column(name = "thumbnail_object_key")
    private String thumbnailObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "thumbnail_status")
    private PozingThumbnailStatus thumbnailStatus = PozingThumbnailStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.thumbnailStatus == null) {
            this.thumbnailStatus = PozingThumbnailStatus.PENDING;
        }
    }

    @Builder
    private Pozing(
            CourseSpot courseSpot,
            User user,
            String pozingObjectKey,
            String thumbnailObjectKey,
            PozingThumbnailStatus thumbnailStatus
    ) {
        this.courseSpot = courseSpot;
        this.user = user;
        this.pozingObjectKey = pozingObjectKey;
        this.thumbnailObjectKey = thumbnailObjectKey;
        this.thumbnailStatus = thumbnailStatus == null ? PozingThumbnailStatus.PENDING : thumbnailStatus;
    }

    public void completeThumbnail(String thumbnailObjectKey) {
        this.thumbnailObjectKey = thumbnailObjectKey;
        this.thumbnailStatus = PozingThumbnailStatus.COMPLETED;
    }

    public void failThumbnail() {
        this.thumbnailStatus = PozingThumbnailStatus.FAILED;
    }
}
