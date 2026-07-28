package com.pozit.pozitserver.course.repository;

import com.pozit.pozitserver.course.domain.TouristSpot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TouristSpotRepository extends JpaRepository<TouristSpot, Long> {

    Optional<TouristSpot> findByContentId(String contentId);

    List<TouristSpot> findByContentIdIn(Collection<String> contentIds);
}
