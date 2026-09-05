package com.pozit.pozitserver.course.repository;

import com.pozit.pozitserver.course.domain.TouristSpot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TouristSpotRepository extends JpaRepository<TouristSpot, Long> {

    Optional<TouristSpot> findByContentId(String contentId);

    List<TouristSpot> findByContentIdIn(Collection<String> contentIds);

    @Query("""
            select ts
            from TouristSpot ts
            left join ts.region r
            where ts.contentId is not null
                and ts.contentId <> ''
                and ts.name is not null
                and ts.name <> ''
                and ts.latitude is not null
                and ts.longitude is not null
                and (:regionCode is null
                    or :regionCode = ''
                    or r.code = :regionCode
                    or r.code like concat(:regionCode, '%')
                    or ts.legalDongRegionCode = :legalDongRegionCode
                    or ts.legalDongSigunguCode = :legalDongSigunguCode)
            order by ts.id asc
            """)
    List<TouristSpot> findRecommendableByRegion(
            @Param("regionCode") String regionCode,
            @Param("legalDongRegionCode") String legalDongRegionCode,
            @Param("legalDongSigunguCode") String legalDongSigunguCode,
            Pageable pageable
    );

    @Query("""
            select ts
            from TouristSpot ts
            where ts.contentId is not null
                and ts.contentId <> ''
                and ts.name is not null
                and ts.name <> ''
                and ts.latitude is not null
                and ts.longitude is not null
            order by ts.id asc
            """)
    List<TouristSpot> findRecommendable(Pageable pageable);

    @Query("""
            select
                ts.id as touristSpotId,
                ts.name as title,
                ts.address as address,
                ts.latitude as latitude,
                ts.longitude as longitude,
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
            group by ts.id, ts.name, ts.address, ts.latitude, ts.longitude, ts.imageUrl
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

        BigDecimal getLatitude();

        BigDecimal getLongitude();

        String getImageUrl();

        long getCourseSpotCount();
    }
}
