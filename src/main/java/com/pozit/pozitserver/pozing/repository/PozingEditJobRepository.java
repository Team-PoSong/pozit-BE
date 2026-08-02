package com.pozit.pozitserver.pozing.repository;

import com.pozit.pozitserver.pozing.domain.PozingEditJob;
import com.pozit.pozitserver.pozing.domain.PozingEditJobStatus;
import com.pozit.pozitserver.travel.domain.Travel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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
}
