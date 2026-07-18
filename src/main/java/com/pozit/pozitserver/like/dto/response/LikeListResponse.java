package com.pozit.pozitserver.like.dto.response;

import java.time.LocalDate;
import java.util.List;

public record LikeListResponse(
        Long travelId,
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String backgroundImageUrl,
        List<String> tags,
        String leaderNickname,
        Integer memberCount,
        Integer likeCount      // ← 찜 수
) {}