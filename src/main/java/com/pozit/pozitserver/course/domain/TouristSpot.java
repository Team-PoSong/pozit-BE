package com.pozit.pozitserver.course.domain;

import com.pozit.pozitserver.travel.domain.Region;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tourist_spots")
public class TouristSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false, length = 50, unique = true)
    private String contentId;

    @Column(name = "content_type_id", length = 10)
    private String contentTypeId;

    @Column(nullable = false)
    private String name;

//    @Column(name = "area_code", length = 30)
//    private String areaCode;

    @Column(name = "legal_dong_region_code", length = 10)
    private String legalDongRegionCode;

    @Column(name = "legal_dong_sigungu_code", length = 10)
    private String legalDongSigunguCode;

//    @Column(name = "sigungu_code", length = 30)
//    private String sigunguCode;

    @Column(length = 500)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 7)
    private BigDecimal longitude;

    @Column(name = "image_url", length = 555)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Builder
    private TouristSpot(
            String contentId,
            String contentTypeId,
            String name,
            String legalDongRegionCode,
            String legalDongSigunguCode,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String imageUrl
    ) {
        this.contentId = contentId;
        this.contentTypeId = contentTypeId;
        this.name = name;
        this.legalDongRegionCode = legalDongRegionCode;
        this.legalDongSigunguCode = legalDongSigunguCode;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
    }
}
