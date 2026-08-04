package com.pozit.pozitserver.pozing.dto.response;

public record PozingSaveResponse(
        Long pozingId,
        Long courseSpotId,
        String pozingObjectKey,
        String pozingUrl,
        String thumbnailUrl
) {
}
