package com.pozit.pozitserver.course.repository;

import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.travel.domain.Travel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTravelOrderByDayNumberAsc(Travel travel);

    List<Course> findByTravelInOrderByDayNumberAsc(List<Travel> travels);
}
