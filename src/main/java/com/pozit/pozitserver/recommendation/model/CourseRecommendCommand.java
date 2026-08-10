package com.pozit.pozitserver.recommendation.model;

import com.pozit.pozitserver.travel.domain.Transportation;
import com.pozit.pozitserver.travel.domain.TravelStyle;

import java.time.LocalDate;
import java.util.List;

public record CourseRecommendCommand(
        Long travelId,
        String destination,
        String regionCode,
        LocalDate startDate,
        LocalDate endDate,
        TravelStyle travelStyle,
        Transportation transportation,
        List<RecommendationTag> tags
) {

    public int travelDays() {
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }
}
