package com.pozit.pozitserver.travel.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.travel.dto.request.TravelUpdateRequest;
import com.pozit.pozitserver.travel.dto.request.TravelVisibilityRequest;
import com.pozit.pozitserver.travel.dto.response.TravelDetailResponse;
import com.pozit.pozitserver.travel.dto.response.TravelListResponse;
import com.pozit.pozitserver.travel.dto.response.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travels")
@RequiredArgsConstructor
@Tag(name = "Travel API")
public class TravelController {

    @GetMapping
    @Operation(summary = "여행 목록 조회", description = "미완료(예정/진행중) 및 완료된 지난 여행 목록을 구분하여 조회합니다.")
    public SuccessResponse<List<TravelListResponse>> getTravels() {
        return SuccessResponse.ok(null);
    }

    @GetMapping("/{travelId}")
    @Operation(summary = "여행 상세 조회", description = "선택한 여행의 일정, 장소 리스트, 타임랩스, 참여 멤버, 진행률을 조회합니다.")
    public SuccessResponse<TravelDetailResponse> getTravelDetail(
            @Parameter(description = "여행 ID") @PathVariable Long travelId) {
        return SuccessResponse.ok(null);
    }

    @PatchMapping("/{travelId}")
    @Operation(summary = "여행 정보 및 설정 수정", description = "배경 사진, 날짜, 태그, 공개 여부 등 여행 정보를 수정합니다.")
    public SuccessResponse<Void> updateTravel(
            @Parameter(description = "여행 ID") @PathVariable Long travelId,
            @RequestBody TravelUpdateRequest request) {
        return SuccessResponse.ok();
    }

    @PatchMapping("/{travelId}/visibility")
    @Operation(summary = "여행 공개 설정", description = "여행 공개/비공개를 설정합니다. 기본은 비공개, 공개 시 코스, 완주율, 배경 사진이 공개됩니다.")
    public SuccessResponse<Void> updateVisibility(
            @Parameter(description = "여행 ID") @PathVariable Long travelId,
            @RequestBody TravelVisibilityRequest request) {
        return SuccessResponse.ok();
    }

    @PostMapping("/{travelId}/background-image")
    @Operation(summary = "배경 사진 업로드 URL 발급", description = "S3 presigned URL을 발급합니다. 클라이언트는 해당 URL로 직접 업로드합니다.")
    public SuccessResponse<PresignedUrlResponse> getBackgroundImageUploadUrl(
            @Parameter(description = "여행 ID") @PathVariable Long travelId) {
        return SuccessResponse.ok(null);
    }
}
