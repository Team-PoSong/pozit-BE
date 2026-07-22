package com.pozit.pozitserver.travel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record TravelUpdateRequest(
        @NotBlank @Size(max = 50) String title,
        @NotBlank @Size(max = 30) String destination,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull List<@NotNull Long> tagIds
) {}