package com.pozit.pozitserver.course.dto.response.coursespot;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "검색 결과 장소 저장 응답")
public record CourseSpotSaveResponse(
        @Schema(description = "저장되었거나 이미 존재하던 관광지 목록")
        List<SavedSpot> spots
) {
    @Schema(description = "저장된 관광지 정보")
    public record SavedSpot(
            @Schema(description = "관광공사 콘텐츠 ID", example = "126508")
            String contentId,

            @Schema(description = "저장되었거나 이미 존재하던 관광지 ID", example = "1")
            Long touristSpotId,

            @Schema(description = "관광지명", example = "경복궁")
            String title,

            @Schema(description = "주소", example = "서울특별시 종로구 사직로 161")
            String address
    ) {
    }
}
