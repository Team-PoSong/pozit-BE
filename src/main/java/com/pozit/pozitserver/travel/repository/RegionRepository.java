package com.pozit.pozitserver.travel.repository;

import com.pozit.pozitserver.travel.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region,Long> {
    List<Region> findAllByActiveTrueOrderBySidoAscSigunguAsc();

    Optional<Region> findByIdAndActiveTrue(Long id);
}
