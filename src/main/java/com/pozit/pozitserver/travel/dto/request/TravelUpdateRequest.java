package com.pozit.pozitserver.travel.dto.request;

import java.time.LocalDate;
import java.util.List;

public record TravelUpdateRequest(
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        List<Long> tagIds,
        Boolean isPublic
) {}
