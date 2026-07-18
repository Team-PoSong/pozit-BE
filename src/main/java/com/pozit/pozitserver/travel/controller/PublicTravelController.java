package com.pozit.pozitserver.travel.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.travel.dto.response.TravelDetailResponse;
import com.pozit.pozitserver.travel.dto.response.TravelListResponse;
import com.pozit.pozitserver.course.dto.response.CourseDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public Travel API")
public class PublicTravelController {

    @GetMapping("/travels")
    @Operation(summary = "공개 여행 목록 조회/검색/필터링", description = "타 사용자들이 공개한 여행 코스 리스트를 조회합니다. 지역, 태그, 키워드 복합 조건으로 검색할 수 있습니다.")
    public SuccessResponse<List<TravelListResponse>> getPublicTravels(
            @Parameter(description = "키워드 (최소 1글자)") @RequestParam(required = false) String keyword,
            @Parameter(description = "태그 ID") @RequestParam(required = false) Long tagId,
            @Parameter(description = "지역명") @RequestParam(required = false) String destination) {
        return SuccessResponse.ok(null);
    }

    @GetMapping("/travels/{travelId}")
    @Operation(summary = "공개 여행 상세 조회", description = "공개된 여행의 상세 정보를 조회합니다.")
    public SuccessResponse<TravelDetailResponse> getPublicTravelDetail(
            @Parameter(description = "여행 ID") @PathVariable Long travelId) {
        return SuccessResponse.ok(null);
    }

    @GetMapping("/courses/{courseId}")
    @Operation(summary = "공개 여행코스 상세 조회", description = "공개된 여행의 일자별 코스를 상세 조회합니다.")
    public SuccessResponse<CourseDetailResponse> getPublicCourseDetail(
            @Parameter(description = "코스 ID") @PathVariable Long courseId) {
        return SuccessResponse.ok(null);
    }
}