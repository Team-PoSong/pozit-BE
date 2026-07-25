package com.pozit.pozitserver.course.dto.response;

import java.util.List;

public record CurrentLocationResponse(
        Double latitude,
        Double longitude,
        List<NearbySpotResponse> nearbySpots
) {}
