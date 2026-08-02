package com.pozit.pozitserver.pozing.domain;

import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "pozing_edit_jobs",
        indexes = {
                @Index(name = "idx_pozing_edit_job_travel_status", columnList = "travel_id,status"),
                @Index(name = "idx_pozing_edit_job_request_user", columnList = "request_user_id")
        }
)
public class PozingEditJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_user_id", nullable = false)
    private User requestUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PozingEditJobStatus status;

    @Column(name = "result_s3_key")
    private String resultS3Key;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public static PozingEditJob queued(Travel travel, User requestUser) {
        PozingEditJob job = new PozingEditJob();
        job.travel = travel;
        job.requestUser = requestUser;
        job.status = PozingEditJobStatus.QUEUED;
        job.retryCount = 0;
        return job;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void start() {
        this.status = PozingEditJobStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void complete(String resultS3Key, LocalDateTime expiresAt) {
        this.status = PozingEditJobStatus.COMPLETED;
        this.resultS3Key = resultS3Key;
        this.completedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.errorMessage = null;
    }

    public void fail(String errorMessage) {
        this.status = PozingEditJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
        this.retryCount++;
    }
}
