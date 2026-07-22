package com.pozit.pozitserver.course.repository;

import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.course.domain.CourseSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseSpotRepository extends JpaRepository<CourseSpot, Long> {

    List<CourseSpot> findByCourseOrderByOrderIndexAsc(Course course);

    long countByCourse_Travel(com.pozit.pozitserver.travel.domain.Travel travel);

    @Query("""
            select cs
            from CourseSpot cs
            join fetch cs.course c
            join fetch cs.touristSpot
            where c in :courses
            order by c.id asc, cs.orderIndex asc
            """)
    List<CourseSpot> findAllByCourseInOrder(@Param("courses") List<Course> courses);
}
