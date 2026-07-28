package com.pozit.pozitserver.course.service;

import com.pozit.pozitserver.course.domain.TouristSpot;
import com.pozit.pozitserver.course.dto.response.coursespot.TourApiResponse;

import java.math.BigDecimal;
import java.time.Instant;

record CachedTouristSpot(
        String contentId,
        String contentTypeId,
        String title,
        String address,
        String imageUrl,
        Double longitude,
        Double latitude,
        String legalDongRegionCode,
        String legalDongSigunguCode,
        Instant expiresAt
) {
    static CachedTouristSpot from(
            TourApiResponse.Response.Item item,
            Instant expiresAt
    ) {
        return new CachedTouristSpot(
                item.contentId(),
                item.contentTypeId(),
                item.title(),
                createAddress(item.addr1(), item.addr2()),
                emptyToNull(item.firstimage()),
                parseDouble(item.mapx()),
                parseDouble(item.mapy()),
                item.legalDongRegionCode(),
                item.legalDongSigunguCode(),
                expiresAt
        );
    }

    TouristSpot toEntity() {
        return TouristSpot.builder()
                .contentId(contentId)
                .contentTypeId(contentTypeId)
                .name(title)
                .legalDongRegionCode(legalDongRegionCode)
                .legalDongSigunguCode(legalDongSigunguCode)
                .address(address)
                .latitude(toBigDecimal(latitude))
                .longitude(toBigDecimal(longitude))
                .imageUrl(imageUrl)
                .build();
    }

    boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private static String createAddress(String addr1, String addr2) {
        if (addr1 == null) {
            return "";
        }

        if (addr2 == null || addr2.isBlank()) {
            return addr1;
        }

        return addr1 + " " + addr2;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
