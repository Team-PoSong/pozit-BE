package com.pozit.pozitserver.travel.dto.response;

import com.pozit.pozitserver.travel.domain.Transportation;
import com.pozit.pozitserver.travel.domain.TravelStyle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "찜 기반 여행 생성 초안 응답")
public record LikeBasedTravelDraftResponse(
        @Schema(description = "원본 여행 ID", example = "1")
        Long sourceTravelId,

        @Schema(description = "여행 제목", example = "서울 감성 여행")
        String title,

        @Schema(description = "여행 목적지명", example = "서울특별시")
        String destination,

        @Schema(description = "여행 목적지 지역 코드", example = "11000")
        String regionCode,

        @Schema(description = "여행 시작일", example = "2026-08-01")
        LocalDate startDate,

        @Schema(description = "여행 종료일", example = "2026-08-03")
        LocalDate endDate,

        @Schema(description = "이동 수단")
        Transportation transportation,

        @Schema(description = "여행 스타일")
        TravelStyle travelStyle,

        @Schema(description = "배경 이미지 URL")
        String backgroundImageUrl,

        @Schema(description = "태그 ID 목록")
        List<Long> tagIds,

        @Schema(description = "일차별 코스 초안")
        List<CourseDraft> courses
) {
    public record CourseDraft(
            @Schema(description = "원본 코스 ID", example = "1")
            Long sourceCourseId,

            @Schema(description = "여행 일차", example = "1")
            Integer dayNumber,

            @Schema(description = "원본 코스 날짜", example = "2026-08-01")
            LocalDate date,

            @Schema(description = "코스 장소 목록")
            List<CourseSpotDraft> spots
    ) {
    }

    public record CourseSpotDraft(
            @Schema(description = "원본 코스 장소 ID", example = "1")
            Long sourceCourseSpotId,

            @Schema(description = "관광지 ID", example = "10")
            Long touristSpotId,

            @Schema(description = "관광지명", example = "경복궁")
            String title,

            @Schema(description = "주소", example = "서울특별시 종로구 사직로 161")
            String address,

            @Schema(description = "대표 이미지 URL", example = "https://...")
            String imageUrl,

            @Schema(description = "정렬 순서", example = "1")
            Integer orderIndex
    ) {
    }
}
