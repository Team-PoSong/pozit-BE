package com.pozit.pozitserver.like.repository;

import com.pozit.pozitserver.like.domain.Like;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    boolean existsByTravelAndUser(Travel travel, User user);

    Optional<Like> findByTravelAndUser(Travel travel, User user);

    void deleteByUser(User user);


    void deleteByTravel(Travel travel);

    long countByTravel(Travel travel);

    List<Like> findByUserAndTravelIn(User user, List<Travel> travels);

    @Query("""
            select l
            from Like l
            join fetch l.travel
            where l.user = :user
            order by l.createdAt desc
            """)
    List<Like> findAllWithTravelByUser(@Param("user") User user);

    @Query("""
            select l.travel.id as travelId, count(l) as likeCount
            from Like l
            where l.travel in :travels
            group by l.travel.id
            """)
    List<TravelLikeCount> countByTravelIn(@Param("travels") List<Travel> travels);

    interface TravelLikeCount {
        Long getTravelId();
        Long getLikeCount();
    }
}
