package com.pozit.pozitserver.pozing.dto.response;

import com.pozit.pozitserver.pozing.domain.PozingThumbnailStatus;

public record PozingThumbnailStatusResponse(
        Long pozingId,
        PozingThumbnailStatus thumbnailStatus,
        String thumbnailUrl
) {
}
