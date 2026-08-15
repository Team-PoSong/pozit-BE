package com.pozit.pozitserver.pozing.repository;

import com.pozit.pozitserver.pozing.domain.TimelapseManifest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TimelapseManifestRepository extends JpaRepository<TimelapseManifest, Long> {

    Optional<TimelapseManifest> findByPozingEditJob_Id(Long pozingEditJobId);
}
