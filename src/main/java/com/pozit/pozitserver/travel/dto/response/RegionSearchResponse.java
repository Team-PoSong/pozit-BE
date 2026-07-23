package com.pozit.pozitserver.travel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지역 검색 결과")
public record RegionSearchResponse (
        @Schema(description = "지역 코드", example = "11000")
        String code,

        @Schema(description = "지역명", example = "서울특별시")
        String name,

        @Schema(description = "상위 시도 코드", example = "11000")
        String provinceCode,

        @Schema(description = "상위 시도명", example = "서울특별시")
        String provinceName
){
}
