package com.pozit.pozitserver.pozing.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PozingSaveRequest(
        @NotBlank
        String objectKey,
        Long courseSpotId
) {
}
