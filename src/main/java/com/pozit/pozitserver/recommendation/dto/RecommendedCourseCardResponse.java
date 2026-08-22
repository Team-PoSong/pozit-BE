package com.pozit.pozitserver.recommendation.dto;

import com.pozit.pozitserver.travel.dto.response.PublicTravelListResponse;

import java.time.LocalDate;
import java.util.List;

public record RecommendedCourseCardResponse(
        String previewId,
        long previewExpiresInSeconds,
        Long travelId,
        String badge,
        String cardTitle,
        String travelTitle,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        int dayCount,
        int nightCount,
        String periodText,
        String thumbnailImageUrl,
        List<String> imageUrls,
        List<String> tags,
        int memberCount,
        int placeCount,
        List<PublicTravelListResponse> relatedPublicTravels
) {
}
