package com.pozit.pozitserver.travel.dto.response;

import com.pozit.pozitserver.travel.domain.Region;

public record RegionResponse(
        Long id,
        String sido,
        String sigungu,
        String fullName
) {

    public static RegionResponse from(Region region) {
        return new RegionResponse(
                region.getId(),
                region.getSido(),
                region.getSigungu(),
                region.getFullName()
        );
    }
}