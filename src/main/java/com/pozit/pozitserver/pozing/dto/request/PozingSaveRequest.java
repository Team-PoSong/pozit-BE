package com.pozit.pozitserver.pozing.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PozingSaveRequest(
        @NotBlank
        String uploadId,

        String thumbnailUrl
) {
}
