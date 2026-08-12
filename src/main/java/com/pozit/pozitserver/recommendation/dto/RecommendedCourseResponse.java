package com.pozit.pozitserver.recommendation.dto;

import java.time.LocalDate;
import java.util.List;

public record RecommendedCourseResponse(
        Long travelId,
        int dayCount,
        List<RecommendedDayResponse> days
) {

    public record RecommendedDayResponse(
            int dayNumber,
            LocalDate date,
            List<RecommendedPlaceResponse> places
    ) {
    }

    public record RecommendedPlaceResponse(
            int orderIndex,
            String contentId,
            String contentTypeId,
            String title,
            String address,
            String imageUrl,
            double latitude,
            double longitude,
            int stayMinutes,
            double finalScore,
            double contentScore,
            double transportationScore,
            double qualityScore
    ) {
    }
}
