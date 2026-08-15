package com.pozit.pozitserver.pozing.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PozingMapManifestResponse(
        int version,
        Long travelId,
        List<CourseMapManifest> courses
) {

    public record CourseMapManifest(
            Long courseId,
            Integer dayNumber,
            List<SpotMapManifest> spots
    ) {
    }

    public record SpotMapManifest(
            Long courseSpotId,
            Long touristSpotId,
            String name,
            Integer orderIndex,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }
}
