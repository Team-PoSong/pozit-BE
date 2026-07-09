package com.pozit.pozitserver.travel.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TravelListResponse(
        Long travelId,
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Boolean isPublic,
        String backgroundImageUrl,
        Integer completionRate,
        List<String> tags,
        String leaderNickname,
        Integer memberCount
) {}
