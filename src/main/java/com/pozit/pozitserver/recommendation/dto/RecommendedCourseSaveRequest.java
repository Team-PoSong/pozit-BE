package com.pozit.pozitserver.recommendation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record RecommendedCourseSaveRequest(
        @NotEmpty List<@Valid RecommendedDaySaveRequest> days
) {

    public record RecommendedDaySaveRequest(
            @Positive int dayNumber,
            @NotEmpty List<@Valid RecommendedPlaceSaveRequest> places
    ) {
    }

    public record RecommendedPlaceSaveRequest(
            @Positive int orderIndex,
            @NotBlank String contentId,
            String contentTypeId,
            @NotBlank String title,
            String address,
            String imageUrl,
            @NotNull Double latitude,
            @NotNull Double longitude,
            String legalDongRegionCode,
            String legalDongSigunguCode
    ) {
    }
}
