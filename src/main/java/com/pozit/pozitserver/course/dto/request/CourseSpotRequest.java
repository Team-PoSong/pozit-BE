package com.pozit.pozitserver.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "검색 결과 장소 저장 요청")
public record CourseSpotRequest(
        @Schema(description = "검색 결과에서 사용자가 선택한 관광공사 콘텐츠 ID 목록", example = "[\"126508\", \"127736\"]")
        @NotEmpty
        List<@NotBlank String> contentIds
) {
}
