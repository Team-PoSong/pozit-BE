package com.pozit.pozitserver.tag.repository;

import com.pozit.pozitserver.tag.domain.TravelTag;
import com.pozit.pozitserver.travel.domain.Travel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TravelTagRepository extends JpaRepository<TravelTag, Long> {

    List<TravelTag> findByTravel(Travel travel);

    @Query("""
            select tt
            from TravelTag tt
            join fetch tt.tag
            where tt.travel in :travels
            """)
    List<TravelTag> findAllWithTagByTravelIn(@Param("travels") List<Travel> travels);
}
