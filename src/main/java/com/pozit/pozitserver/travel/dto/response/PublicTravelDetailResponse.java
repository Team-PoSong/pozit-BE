package com.pozit.pozitserver.travel.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PublicTravelDetailResponse(
        Long travelId,
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Boolean isPublic,
        String backgroundImageUrl,
        String leaderNickname,
        Integer memberCount,
        Integer completionRate,
        Integer totalSpotCount,
        Integer totalPozingCount,
        List<String> tags,
        List<CourseInfo> courses
) {
    public record CourseInfo(
            Long courseId,
            Integer dayNumber,
            LocalDate date,
            List<CourseSpotInfo> spots
    ) {}

    public record CourseSpotInfo(
            Long courseSpotId,
            Long touristSpotId,
            String name,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer orderIndex,
            String status,
            List<PublicPozingInfo> pozings
    ) {}

    public record PublicPozingInfo(
            Long pozingId,
            String nickname,
            String pozingUrl,
            String thumbnailUrl
    ) {}
}
