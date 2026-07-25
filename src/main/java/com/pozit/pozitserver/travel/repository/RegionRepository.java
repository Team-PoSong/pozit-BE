package com.pozit.pozitserver.travel.repository;

import com.pozit.pozitserver.travel.domain.Region;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region,Long> {
    @Query("""
        select r
        from Region r
        join fetch r.parent p
        where r.parent is not null
          and r.name like concat('%', :keyword, '%')
        order by r.name asc
        """)
    List<Region> search(
            @Param("keyword") String keyword,
            Pageable pageable
    );

}
