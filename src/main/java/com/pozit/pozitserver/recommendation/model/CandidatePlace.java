package com.pozit.pozitserver.recommendation.model;

import com.pozit.pozitserver.course.dto.response.coursespot.TourApiResponse;

public record CandidatePlace(
        String contentId,
        String contentTypeId,
        String title,
        String address,
        String imageUrl,
        String mapX,
        String mapY,
        String tel,
        String legalDongRegionCode,
        String legalDongSigunguCode
) {

    public static CandidatePlace from(TourApiResponse.Response.Item item) {
        return new CandidatePlace(
                trimToNull(item.contentId()),
                trimToNull(item.contentTypeId()),
                trimToNull(item.title()),
                joinAddress(item.addr1(), item.addr2()),
                firstNotBlank(item.firstimage(), item.firstimage2()),
                trimToNull(item.mapx()),
                trimToNull(item.mapy()),
                trimToNull(item.tel()),
                trimToNull(item.legalDongRegionCode()),
                trimToNull(item.legalDongSigunguCode())
        );
    }

    public boolean hasCoordinate() {
        return mapX != null && mapY != null;
    }

    public double longitude() {
        return Double.parseDouble(mapX);
    }

    public double latitude() {
        return Double.parseDouble(mapY);
    }

    public boolean hasImage() {
        return imageUrl != null;
    }

    public boolean hasAddress() {
        return address != null;
    }

    private static String joinAddress(String addr1, String addr2) {
        String first = trimToNull(addr1);
        String second = trimToNull(addr2);

        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first + " " + second;
    }

    private static String firstNotBlank(String first, String second) {
        String normalizedFirst = trimToNull(first);
        return normalizedFirst != null ? normalizedFirst : trimToNull(second);
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
