package com.pozit.pozitserver.travel.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TravelDetailResponse(
        Long travelId,
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Boolean isPublic,
        String backgroundImageUrl,
        String inviteCode,
        Integer completionRate,
        Integer totalSpotCount,
        Integer totalPozingCount,
        List<String> tags,
        List<MemberInfo> members,
        List<CourseInfo> courses
) {
    public record MemberInfo(Long userId, String nickname, String role) {}

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
            List<PozingInfo> pozings
    ) {}

    public record PozingInfo(
            Long pozingId,
            Long userId,
            String nickname,
            String pozingUrl,
            String thumbnailUrl
    ) {}
}
