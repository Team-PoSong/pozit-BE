package com.pozit.pozitserver.pozing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PozingSaveRequest(
        @NotNull
        Long courseSpotId,

        @NotBlank
        String pozingUrl,

        String thumbnailUrl
) {
}
