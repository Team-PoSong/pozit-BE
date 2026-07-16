package com.pozit.pozitserver.course.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.course.dto.request.CourseSpotAddRequest;
import com.pozit.pozitserver.course.dto.request.CourseSpotOrderRequest;
import com.pozit.pozitserver.course.dto.request.LocationRequest;
import com.pozit.pozitserver.course.dto.response.CourseDetailResponse;
import com.pozit.pozitserver.course.dto.response.CourseListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Course API")
public class CourseController {

    @GetMapping("/{courseId}")
    @Operation(summary = "코스 상세 조회", description = "일자별 코스의 장소 리스트와 방문 상태를 조회합니다.")
    public SuccessResponse<CourseDetailResponse> getCourseDetail(
            @Parameter(description = "코스 ID") @PathVariable Long courseId) {
        return SuccessResponse.ok(null);
    }

    @PatchMapping("/{courseId}/current-location")
    @Operation(summary = "현재 위치 갱신", description = "사용자 현재 GPS 위치를 갱신하고, 관광지 방문 상태를 전이합니다.")
    public SuccessResponse<Void> updateLocation(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @RequestBody LocationRequest request) {
        return SuccessResponse.ok();
    }

    @PostMapping("/{courseId}/spots")
    @Operation(summary = "코스 장소 추가", description = "팀장이 일자별 코스에 장소를 추가합니다.")
    public SuccessResponse<Void> addCourseSpot(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @RequestBody CourseSpotAddRequest request) {
        return SuccessResponse.ok();
    }

    @DeleteMapping("/{courseId}/spots/{spotId}")
    @Operation(summary = "코스 장소 삭제", description = "팀장이 코스에서 장소를 삭제합니다. 해당 장소의 포징도 함께 삭제됩니다.")
    public SuccessResponse<Void> deleteCourseSpot(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @Parameter(description = "코스 장소 ID") @PathVariable Long spotId) {
        return SuccessResponse.ok();
    }

    @PatchMapping("/{courseId}/spots/order")
    @Operation(summary = "코스 장소 순서 변경", description = "팀장이 일자별 코스의 방문 순서를 변경합니다.")
    public SuccessResponse<Void> updateCourseSpotOrder(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @RequestBody CourseSpotOrderRequest request) {
        return SuccessResponse.ok();
    }
}