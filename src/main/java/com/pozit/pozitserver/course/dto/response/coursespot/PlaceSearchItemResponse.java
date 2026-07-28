package com.pozit.pozitserver.course.dto.response.coursespot;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlaceSearchItemResponse(
        @JsonProperty("contentid")
        String contentId,

        @JsonProperty("contenttypeid")
        String contentTypeId,
        String title,
        String address,
        String imageUrl,
        Double longitude,
        Double latitude
) {
    public static PlaceSearchItemResponse from(
            TourApiResponse.Response.Item item
    ) {
        return new PlaceSearchItemResponse(
                item.contentId(),
                item.contentTypeId(),
                item.title(),
                createAddress(item.addr1(), item.addr2()),
                emptyToNull(item.firstimage()),
                parseDouble(item.mapx()),
                parseDouble(item.mapy())
        );
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
