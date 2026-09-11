package com.pozit.pozitserver.recommendation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RecommendedTravelStartRequest(
        @NotBlank String previewId,
        @Valid @NotEmpty List<RecommendedCourseSaveRequest.RecommendedDaySaveRequest> days
) {
}
