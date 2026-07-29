package com.pozit.pozitserver.travel.dto.request;

import com.pozit.pozitserver.travel.domain.Transportation;
import com.pozit.pozitserver.travel.domain.TravelStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "찜 기반 여행 최종 생성 요청")
public record LikeBasedTravelCreateRequest(
        @Schema(description = "원본 여행 ID", example = "1")
        @NotNull
        Long sourceTravelId,

        @Schema(description = "여행 제목", example = "내가 가는 서울 여행", maxLength = 50)
        @NotBlank
        @Size(max = 50)
        String title,

        @Schema(description = "여행 목적지명", example = "서울특별시")
        @NotBlank
        String destination,

        @Schema(description = "여행 목적지 지역 코드", example = "11000")
        @NotBlank
        String regionCode,

        @Schema(description = "여행 시작일", example = "2026-08-10")
        @NotNull
        LocalDate startDate,

        @Schema(description = "여행 종료일", example = "2026-08-12")
        @NotNull
        LocalDate endDate,

        @Schema(description = "이동 수단")
        Transportation transportation,

        @Schema(description = "여행 스타일")
        TravelStyle travelStyle,

        @Schema(description = "배경 이미지 URL")
        String backgroundImageUrl,

        @Schema(description = "여행 태그 ID 목록", example = "[1, 2, 3]")
        @NotNull
        List<@NotNull Long> tagIds,

        @Schema(description = "일차별 코스 장소 목록")
        @NotEmpty
        List<@Valid CourseRequest> courses
) {
    public record CourseRequest(
            @Schema(description = "여행 일차", example = "1")
            @NotNull
            Integer dayNumber,

            @Schema(description = "해당 일차에 등록할 관광지 ID 목록", example = "[10, 11, 12]")
            @NotNull
            List<@NotNull Long> touristSpotIds
    ) {
    }
}
