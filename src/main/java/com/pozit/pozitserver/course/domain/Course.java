package com.pozit.pozitserver.course.domain;

import com.pozit.pozitserver.travel.domain.Travel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(nullable = false)
    private LocalDate date;

    @Builder
    private Course(Travel travel, Integer dayNumber, LocalDate date) {
        this.travel = travel;
        this.dayNumber = dayNumber;
        this.date = date;
    }

    public void updateDate(LocalDate date) {
        this.date = date;
    }
}
