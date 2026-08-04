package com.pozit.pozitserver.course.dto.response.coursespot;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인기 관광지 랭킹 응답")
public record HostTouristSpotRankResponse(
        @Schema(description = "랭킹 순위", example = "1")
        int rank,

        @Schema(description = "관광지 ID", example = "1")
        Long touristSpotId,

        @Schema(description = "관광지명", example = "경복궁")
        String title,

        @Schema(description = "주소", example = "서울특별시 종로구 사직로 161")
        String address,

        @Schema(description = "대표 이미지 URL", example = "https://...")
        String imageUrl,

        @Schema(description = "코스에 등록된 횟수", example = "12")
        long courseSpotCount
) {
}
