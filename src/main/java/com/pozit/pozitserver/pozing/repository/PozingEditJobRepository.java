package com.pozit.pozitserver.pozing.repository;

import com.pozit.pozitserver.pozing.domain.PozingEditJob;
import com.pozit.pozitserver.pozing.domain.PozingEditJobStatus;
import com.pozit.pozitserver.travel.domain.Travel;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PozingEditJobRepository extends JpaRepository<PozingEditJob, Long> {

    boolean existsByTravelAndStatusIn(Travel travel, Collection<PozingEditJobStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select j
            from PozingEditJob j
            join fetch j.travel
            join fetch j.requestUser
            where j.id = :id
            """)
    Optional<PozingEditJob> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select j
            from PozingEditJob j
            join fetch j.travel
            where j.id = :id
            """)
    Optional<PozingEditJob> findByIdWithTravel(@Param("id") Long id);

    @Query("""
            select j
            from PozingEditJob j
            where j.status = :status
              and j.expiresAt <= :now
              and j.resultS3Key is not null
            order by j.expiresAt asc, j.id asc
            """)
    List<PozingEditJob> findExpiredCompletedJobs(
            @Param("status") PozingEditJobStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
            select j
            from PozingEditJob j
            where j.status = :status
              and j.startedAt is not null
              and j.startedAt <= :threshold
            order by j.startedAt asc, j.id asc
            """)
    List<PozingEditJob> findStaleProcessingJobs(
            @Param("status") PozingEditJobStatus status,
            @Param("threshold") LocalDateTime threshold
    );
}
