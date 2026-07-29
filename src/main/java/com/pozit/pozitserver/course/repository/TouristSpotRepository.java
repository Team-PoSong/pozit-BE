package com.pozit.pozitserver.course.repository;

import com.pozit.pozitserver.course.domain.TouristSpot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TouristSpotRepository extends JpaRepository<TouristSpot, Long> {

    Optional<TouristSpot> findByContentId(String contentId);

    List<TouristSpot> findByContentIdIn(Collection<String> contentIds);

    @Query("""
            select
                ts.id as touristSpotId,
                ts.name as title,
                ts.address as address,
                ts.imageUrl as imageUrl,
                count(cs.id) as courseSpotCount
            from CourseSpot cs
            join cs.touristSpot ts
            left join ts.region r
            where (:regionCode is null
                or :regionCode = ''
                or r.code = :regionCode
                or r.code like concat(:regionCode, '%')
                or ts.legalDongRegionCode = :legalDongRegionCode
                or ts.legalDongSigunguCode = :legalDongSigunguCode)
            group by ts.id, ts.name, ts.address, ts.imageUrl
            order by count(cs.id) desc, ts.id asc
            """)
    List<TouristSpotRankProjection> findHostTouristSpotsRank(
            @Param("regionCode") String regionCode,
            @Param("legalDongRegionCode") String legalDongRegionCode,
            @Param("legalDongSigunguCode") String legalDongSigunguCode,
            Pageable pageable
    );

    interface TouristSpotRankProjection {
        Long getTouristSpotId();

        String getTitle();

        String getAddress();

        String getImageUrl();

        long getCourseSpotCount();
    }
}
