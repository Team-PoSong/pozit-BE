package com.pozit.pozitserver.recommendation.model;

import java.util.List;

public record CourseChatIntent(
        CourseChatAction action,
        int targetDayNumber,
        List<String> removeTerms,
        int removePlaceCount,
        List<String> addKeywords,
        int addPlaceCount,
        boolean optimizeRoute,
        String assistantMessage
) {
}
