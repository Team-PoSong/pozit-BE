package com.pozit.pozitserver.course.dto.response;

import java.time.LocalDate;
import java.util.List;

public record CourseDetailResponse(
        Long courseId,
        Integer dayNumber,
        LocalDate date,
        List<CourseSpotDetail> spots
) {
    public record CourseSpotDetail(
            Long courseSpotId,
            Long touristSpotId,
            String name,
            String address,
            Double latitude,
            Double longitude,
            String imageUrl,
            Integer orderIndex,
            String status,
            List<PozingInfo> pozings
    ) {}

    public record PozingInfo(
            Long pozingId,
            String pozingUrl,
            String thumbnailUrl,
            String nickname
    ) {}
}
