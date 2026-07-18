package com.pozit.pozitserver.travel.dto.response;

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
    public record CourseInfo(Long courseId, Integer dayNumber, LocalDate date) {}
}
