package com.pozit.pozitserver.travel.domain;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.tag.domain.Tag;
import com.pozit.pozitserver.tag.domain.TravelTag;
import com.pozit.pozitserver.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "travels")
public class Travel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 30)
    private String destination;

    @Column(nullable=false)
    private String regionCode;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TravelStatus status;

    @Enumerated(EnumType.STRING)
    @Column
    private Transportation transportation;

    @Enumerated(EnumType.STRING)
    @Column
    private TravelStyle travelStyle;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "invite_code", length = 5, unique = true)
    private String inviteCode;

    @Column(name = "background_image_url")
    private String backgroundImageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TravelStatus.BEFORE;
        }
        if (this.isPublic == null) {
            this.isPublic = false;
        }
    }

    @Builder
    private Travel(
            User leader,
            String title,
            String destination,
            String regionCode,
            LocalDate startDate,
            LocalDate endDate,
            Transportation transportation,
            TravelStyle travelStyle,
            String inviteCode
    ) {
//        validateDateRange(startDate, endDate);
        this.leader = leader;
        this.title = title;
        this.destination = destination;
        this.regionCode=regionCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = TravelStatus.BEFORE;
        this.transportation=transportation;
        this.travelStyle=travelStyle;
        this.inviteCode=inviteCode;
        this.isPublic = false;
    }

    public void updateInfo(String title, String destination, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        this.title = title;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_TRAVEL_PERIOD);
        }
    }

    public void updateVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void updateBackgroundImage(String backgroundImageUrl) {
        this.backgroundImageUrl = backgroundImageUrl;
    }

    public void changeStatus(TravelStatus status) {
        this.status = status;
    }

    public boolean isPubliclyVisible() {
        return this.status == TravelStatus.DONE && Boolean.TRUE.equals(this.isPublic);
    }
}
