package com.pozit.pozitserver.travel.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ActiveSpotsResponse(
        List<SpotInfo> spots
) {
    public record SpotInfo(
            Long travelId,
            Long courseSpotId,
            Long touristSpotId,
            String name,
            BigDecimal latitude,
            BigDecimal longitude
    ) {}
}
