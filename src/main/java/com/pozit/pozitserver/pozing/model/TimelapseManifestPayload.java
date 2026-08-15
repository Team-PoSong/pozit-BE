package com.pozit.pozitserver.pozing.model;

import java.math.BigDecimal;
import java.util.List;

public record TimelapseManifestPayload(
        int version,
        Long travelId,
        List<CourseManifest> courses
) {

    public record CourseManifest(
            Long courseId,
            Integer dayNumber,
            List<SpotManifest> spots
    ) {
    }

    public record SpotManifest(
            Long courseSpotId,
            Long touristSpotId,
            String name,
            Integer orderIndex,
            BigDecimal latitude,
            BigDecimal longitude,
            List<MemberPozingManifest> pozings
    ) {
    }

    public record MemberPozingManifest(
            Long userId,
            String nickname,
            String pozingObjectKey
    ) {
    }
}
