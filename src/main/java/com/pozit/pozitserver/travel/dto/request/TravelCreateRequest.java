package com.pozit.pozitserver.travel.dto.request;

import com.pozit.pozitserver.travel.validator.ValidTravelPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@ValidTravelPeriod
@Schema(description = "여행 생성 요청")
public record TravelCreateRequest(
        @Schema(description = "여행 제목", example = "서울 주말 여행", maxLength = 50)
        @NotBlank @Size(max = 50) String title,

        @Schema(description = "여행 목적지명", example = "서울특별시")
        @NotBlank String destination,

        @Schema(description = "여행 목적지 지역 코드", example = "11000")
        @NotBlank String regionCode,

        @Schema(description = "여행 시작일", example = "2026-08-01")
        @NotNull LocalDate startDate,

        @Schema(description = "여행 종료일", example = "2026-08-03")
        @NotNull LocalDate endDate,

        @Schema(description = "여행 태그 ID 목록", example = "[1, 2, 3]")
        @NotNull List<@NotNull Long> tagIds
) {}
