package com.pozit.pozitserver.pozing.dto.response;

import com.pozit.pozitserver.pozing.domain.PozingThumbnailStatus;

public record PozingSaveResponse(
        Long pozingId,
        Long courseSpotId,
        String pozingObjectKey,
        String pozingUrl,
        String thumbnailUrl,
        PozingThumbnailStatus thumbnailStatus
) {
}
