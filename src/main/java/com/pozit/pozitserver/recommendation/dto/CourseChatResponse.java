package com.pozit.pozitserver.recommendation.dto;

import com.pozit.pozitserver.recommendation.model.CourseChatAction;

import java.time.LocalDate;
import java.util.List;

public record CourseChatResponse(
        CourseChatAction action,
        String assistantMessage,
        int targetDayNumber,
        List<ChatDayResponse> suggestedDays,
        List<String> changes,
        RecommendedCourseSaveRequest commitRequest
) {

    public record ChatDayResponse(
            Long courseId,
            int dayNumber,
            LocalDate date,
            List<ChatPlaceResponse> places
    ) {
    }

    public record ChatPlaceResponse(
            Long courseSpotId,
            Long touristSpotId,
            int orderIndex,
            String contentId,
            String contentTypeId,
            String title,
            String address,
            String imageUrl,
            Double latitude,
            Double longitude,
            String legalDongRegionCode,
            String legalDongSigunguCode,
            boolean newlyAdded
    ) {
    }
}
