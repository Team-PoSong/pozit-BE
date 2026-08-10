package com.pozit.pozitserver.pozing.repository;

import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PozingRepository extends JpaRepository<Pozing, Long> {

    List<Pozing> findByCourseSpotIn(List<CourseSpot> courseSpots);

    List<Pozing> findByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Pozing p
            where p.id = :id
            """)
    Optional<Pozing> findByIdForUpdate(@Param("id") Long id);

    long countByCourseSpot_Course_Travel(Travel travel);

    boolean existsByThumbnailObjectKey(String thumbnailObjectKey);

    @Query("""
            select count(distinct p.user.id)
            from Pozing p
            where p.courseSpot = :courseSpot
            """)
    long countDistinctUserByCourseSpot(@Param("courseSpot") CourseSpot courseSpot);

    @Query("""
            select p
            from Pozing p
            join fetch p.user
            join fetch p.courseSpot cs
            where p.courseSpot in :courseSpots
            order by cs.id asc, p.createdAt asc, p.id asc
            """)
    List<Pozing> findAllWithUserByCourseSpotIn(@Param("courseSpots") List<CourseSpot> courseSpots);

    @Query("""
            select p
            from Pozing p
            join fetch p.user
            join fetch p.courseSpot cs
            join fetch cs.course c
            where c.travel.id = :travelId
            order by c.dayNumber asc, cs.orderIndex asc, p.createdAt asc, p.id asc
            """)
    List<Pozing> findAllByTravelIdForEdit(@Param("travelId") Long travelId);
}
