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
        String legalDongSigunguCode,
        String cat1,
        String cat2,
        String cat3,
        String overview,
        String homepage,
        String restDate,
        String useTime,
        String parking,
        String experienceGuide,
        String eventStartDate,
        String eventEndDate,
        String playTime,
        String foodOpenTime,
        String foodRestDate,
        String foodParking,
        String firstMenu,
        String treatMenu,
        String repeatedInfo
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
                trimToNull(item.legalDongSigunguCode()),
                trimToNull(item.cat1()),
                trimToNull(item.cat2()),
                trimToNull(item.cat3()),
                trimToNull(item.overview()),
                trimToNull(item.homepage()),
                firstNotBlank(item.restdate(), item.restdateculture()),
                firstNotBlank(item.usetime(), item.opentime()),
                firstNotBlank(item.parking(), item.parkingculture()),
                trimToNull(item.expguide()),
                trimToNull(item.eventstartdate()),
                trimToNull(item.eventenddate()),
                firstNotBlank(item.playtime(), item.usetimefestival()),
                trimToNull(item.opentimefood()),
                trimToNull(item.restdatefood()),
                trimToNull(item.parkingfood()),
                trimToNull(item.firstmenu()),
                trimToNull(item.treatmenu()),
                joinInfo(item.infoname(), item.infotext())
        );
    }

    public CandidatePlace merge(CandidatePlace other) {
        return new CandidatePlace(
                firstNotBlank(other.contentId, contentId),
                firstNotBlank(other.contentTypeId, contentTypeId),
                firstNotBlank(other.title, title),
                firstNotBlank(other.address, address),
                firstNotBlank(other.imageUrl, imageUrl),
                firstNotBlank(other.mapX, mapX),
                firstNotBlank(other.mapY, mapY),
                firstNotBlank(other.tel, tel),
                firstNotBlank(other.legalDongRegionCode, legalDongRegionCode),
                firstNotBlank(other.legalDongSigunguCode, legalDongSigunguCode),
                firstNotBlank(other.cat1, cat1),
                firstNotBlank(other.cat2, cat2),
                firstNotBlank(other.cat3, cat3),
                firstNotBlank(other.overview, overview),
                firstNotBlank(other.homepage, homepage),
                firstNotBlank(other.restDate, restDate),
                firstNotBlank(other.useTime, useTime),
                firstNotBlank(other.parking, parking),
                firstNotBlank(other.experienceGuide, experienceGuide),
                firstNotBlank(other.eventStartDate, eventStartDate),
                firstNotBlank(other.eventEndDate, eventEndDate),
                firstNotBlank(other.playTime, playTime),
                firstNotBlank(other.foodOpenTime, foodOpenTime),
                firstNotBlank(other.foodRestDate, foodRestDate),
                firstNotBlank(other.foodParking, foodParking),
                firstNotBlank(other.firstMenu, firstMenu),
                firstNotBlank(other.treatMenu, treatMenu),
                firstNotBlank(other.repeatedInfo, repeatedInfo)
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

    public boolean hasOverview() {
        return overview != null;
    }

    public boolean hasOperatingInfo() {
        return useTime != null
                || restDate != null
                || foodOpenTime != null
                || foodRestDate != null
                || playTime != null;
    }

    public boolean hasParkingInfo() {
        return parking != null || foodParking != null;
    }

    public String searchableText() {
        return String.join(" ",
                nullToEmpty(title),
                nullToEmpty(overview),
                nullToEmpty(experienceGuide),
                nullToEmpty(repeatedInfo),
                nullToEmpty(firstMenu),
                nullToEmpty(treatMenu)
        );
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

    private static String joinInfo(String name, String text) {
        String normalizedName = trimToNull(name);
        String normalizedText = trimToNull(text);

        if (normalizedName == null) {
            return normalizedText;
        }
        if (normalizedText == null) {
            return normalizedName;
        }
        return normalizedName + " " + normalizedText;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
