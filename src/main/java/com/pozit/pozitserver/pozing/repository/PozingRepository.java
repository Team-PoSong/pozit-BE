package com.pozit.pozitserver.pozing.repository;

import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.travel.domain.Travel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PozingRepository extends JpaRepository<Pozing, Long> {

    List<Pozing> findByCourseSpotIn(List<CourseSpot> courseSpots);

    long countByCourseSpot_Course_Travel(Travel travel);

    @Query("""
            select p
            from Pozing p
            join fetch p.user
            join fetch p.courseSpot cs
            where p.courseSpot in :courseSpots
            order by cs.id asc, p.createdAt asc, p.id asc
            """)
    List<Pozing> findAllWithUserByCourseSpotIn(@Param("courseSpots") List<CourseSpot> courseSpots);
}
