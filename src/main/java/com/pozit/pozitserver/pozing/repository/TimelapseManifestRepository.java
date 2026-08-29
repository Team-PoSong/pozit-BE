package com.pozit.pozitserver.pozing.repository;

import com.pozit.pozitserver.pozing.domain.TimelapseManifest;
import com.pozit.pozitserver.travel.domain.Travel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimelapseManifestRepository extends JpaRepository<TimelapseManifest, Long> {

    Optional<TimelapseManifest> findByPozingEditJob_Id(Long pozingEditJobId);

    List<TimelapseManifest> findByTravel(Travel travel);
}
