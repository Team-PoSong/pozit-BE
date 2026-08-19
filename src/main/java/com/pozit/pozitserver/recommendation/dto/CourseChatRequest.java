package com.pozit.pozitserver.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseChatRequest(
        @NotBlank
        @Size(max = 300)
        String message
) {
}
