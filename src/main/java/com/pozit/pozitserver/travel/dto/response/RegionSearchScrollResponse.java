package com.pozit.pozitserver.travel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "지역 검색 무한스크롤 응답")
public record RegionSearchScrollResponse(
        @Schema(description = "현재 커서", example = "1")
        int currentCursor,

        @Schema(description = "다음 커서. 다음 데이터가 없으면 null", example = "2")
        Integer nextCursor,

        @Schema(description = "다음 데이터 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "응답으로 반환된 지역 수", example = "10")
        int size,

        @Schema(description = "지역 검색 결과 목록")
        List<RegionSearchResponse> regions
) {
}
