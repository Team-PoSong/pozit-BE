package com.pozit.pozitserver.course.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_spots")
public class CourseSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tourist_spot_id", nullable = false)
    private TouristSpot touristSpot;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseSpotStatus status;

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = CourseSpotStatus.NOT_VISITED;
        }
    }

    @Builder
    private CourseSpot(Course course, TouristSpot touristSpot, Integer orderIndex) {
        this.course = course;
        this.touristSpot = touristSpot;
        this.orderIndex = orderIndex;
        this.status = CourseSpotStatus.NOT_VISITED;
    }

    public void updateOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public void updateStatus(CourseSpotStatus status) {
        this.status = status;
    }
}
