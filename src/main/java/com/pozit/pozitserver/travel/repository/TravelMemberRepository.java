package com.pozit.pozitserver.travel.repository;

import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelMemberRepository extends JpaRepository<TravelMember, Long> {

    List<TravelMember> findByTravel(Travel travel);

    @Query("""
            select tm
            from TravelMember tm
            join fetch tm.travel
            join fetch tm.user
            where tm.travel in :travels
            """)
    List<TravelMember> findAllWithUserByTravelIn(@Param("travels") List<Travel> travels);

    @Query("""
            select tm
            from TravelMember tm
            join fetch tm.travel
            where tm.user = :user
            """)
    List<TravelMember> findAllWithTravelByUser(@Param("user") User user);

    Optional<TravelMember> findByTravelAndUser(Travel travel, User user);

    boolean existsByTravelAndUser(Travel travel, User user);
}
