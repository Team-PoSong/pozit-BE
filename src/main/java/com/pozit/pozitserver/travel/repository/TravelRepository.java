package com.pozit.pozitserver.travel.repository;

import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelStatus;
import com.pozit.pozitserver.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TravelRepository extends JpaRepository<Travel, Long>, TravelRepositoryCustom {

    Optional<Travel> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    List<Travel> findByLeader(User leader);

    List<Travel> findByLeaderAndStatusNot(User leader, TravelStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Travel t where t.id = :id")
    Optional<Travel> findByIdForUpdate(@Param("id") Long id);

    List<Travel> findByStartDate(LocalDate startDate);

    List<Travel> findByStatusNotAndEndDateBefore(TravelStatus status, LocalDate endDate);
}
