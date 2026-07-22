package com.pozit.pozitserver.travel.repository;

import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelStatus;
import com.pozit.pozitserver.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TravelRepository extends JpaRepository<Travel, Long> {

    Optional<Travel> findByInviteCode(String inviteCode);

    List<Travel> findByLeaderAndStatusNot(User leader, TravelStatus status);
}
