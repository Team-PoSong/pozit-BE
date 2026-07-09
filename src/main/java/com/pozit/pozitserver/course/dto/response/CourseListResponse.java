package com.pozit.pozitserver.course.dto.response;

import java.time.LocalDate;
import java.util.List;

public record CourseListResponse(
        Long courseId,
        Integer dayNumber,
        LocalDate date,
        List<CourseSpotInfo> spots
) {
    public record CourseSpotInfo(
            Long courseSpotId,
            Long touristSpotId,
            String name,
            Double latitude,
            Double longitude,
            Integer orderIndex,
            String status
    ) {}
}
