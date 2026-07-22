package com.pozit.pozitserver.travel.dto.request;

import jakarta.validation.constraints.NotNull;

public record TravelVisibilityRequest(
        @NotNull Boolean isPublic
) {}