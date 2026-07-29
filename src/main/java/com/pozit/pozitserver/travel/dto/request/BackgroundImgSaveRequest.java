package com.pozit.pozitserver.travel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BackgroundImgSaveRequest(
        @NotNull
        Long travelId,

        @NotBlank
        String backGroundImgUrl
) {
}
