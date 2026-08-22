package com.pozit.pozitserver.recommendation.dto;

import java.time.LocalDate;
import java.util.List;

public record RecommendedCourseCardResponse(
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
        List<PreviewPlaceResponse> previewPlaces,
        RecommendedCourseResponse recommendedCourse
) {

    public record PreviewPlaceResponse(
            int dayNumber,
            int orderIndex,
            String title,
            String address,
            String imageUrl
    ) {
    }
}
