package com.pozit.pozitserver.course.repository;

import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.travel.domain.Travel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTravelOrderByDayNumberAsc(Travel travel);

    List<Course> findByTravelInOrderByDayNumberAsc(List<Travel> travels);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);
}
