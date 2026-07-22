package com.pozit.pozitserver.course.domain;

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

    @Column(name = "area_code", length = 30)
    private String areaCode;

    @Column(name = "sigungu_code", length = 30)
    private String sigunguCode;

    @Column(length = 500)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 7)
    private BigDecimal longitude;

    @Column(name = "image_url", length = 555)
    private String imageUrl;

    @Builder
    private TouristSpot(
            String contentId,
            String contentTypeId,
            String name,
            String areaCode,
            String sigunguCode,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String imageUrl
    ) {
        this.contentId = contentId;
        this.contentTypeId = contentTypeId;
        this.name = name;
        this.areaCode = areaCode;
        this.sigunguCode = sigunguCode;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
    }
}
