package com.pozit.pozitserver.pozing.domain;

import com.pozit.pozitserver.travel.domain.Travel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "timelapse_manifests",
        indexes = {
                @Index(name = "idx_timelapse_manifest_travel", columnList = "travel_id"),
                @Index(name = "idx_timelapse_manifest_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_timelapse_manifest_pozing_edit_job",
                        columnNames = "pozing_edit_job_id"
                )
        }
)
public class TimelapseManifest {

    public static final int CURRENT_VERSION = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pozing_edit_job_id", nullable = false, unique = true)
    private PozingEditJob pozingEditJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @Column(name = "manifest_version", nullable = false)
    private Integer manifestVersion;

    @Column(name = "manifest_json", nullable = false, columnDefinition = "TEXT")
    private String manifestJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static TimelapseManifest create(
            PozingEditJob pozingEditJob,
            Travel travel,
            String manifestJson
    ) {
        TimelapseManifest manifest = new TimelapseManifest();
        manifest.pozingEditJob = pozingEditJob;
        manifest.travel = travel;
        manifest.manifestVersion = CURRENT_VERSION;
        manifest.manifestJson = manifestJson;
        return manifest;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
