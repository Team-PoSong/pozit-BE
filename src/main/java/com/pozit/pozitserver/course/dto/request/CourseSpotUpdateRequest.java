package com.pozit.pozitserver.course.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CourseSpotUpdateRequest(
        @NotNull List<@NotNull @Positive Long> touristSpotIds
) {}
