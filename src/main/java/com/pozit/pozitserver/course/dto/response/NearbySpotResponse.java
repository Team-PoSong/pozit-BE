package com.pozit.pozitserver.course.dto.response;

public record NearbySpotResponse(
        Long courseSpotId,
        Long touristSpotId,
        String name,
        Double distanceMeters
) {}
