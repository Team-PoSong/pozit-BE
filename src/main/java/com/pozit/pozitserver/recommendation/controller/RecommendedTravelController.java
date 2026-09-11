package com.pozit.pozitserver.recommendation.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.recommendation.dto.RecommendedCourseCardResponse;
import com.pozit.pozitserver.recommendation.dto.RecommendedCourseResponse;
import com.pozit.pozitserver.recommendation.dto.RecommendedTravelStartRequest;
import com.pozit.pozitserver.recommendation.service.CourseRecommendationService;
import com.pozit.pozitserver.travel.dto.request.TravelCreateRequest;
import com.pozit.pozitserver.travel.dto.response.TravelCreateResponse;
import com.pozit.pozitserver.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations/travels")
@RequiredArgsConstructor
@Tag(name = "Recommended Travel API")
public class RecommendedTravelController {

    private final CourseRecommendationService courseRecommendationService;

    @PostMapping("/preview/card")
    @Operation(
            summary = "포짓 추천 여행 카드 미리보기",
            description = "여행 생성 입력값으로 추천 코스를 생성하고 previewId를 반환합니다. 이 시점에는 여행을 DB에 생성하지 않습니다."
    )
    public SuccessResponse<RecommendedCourseCardResponse> previewDraftCard(
            @CurrentUser User currentUser,
            @Valid @RequestBody TravelCreateRequest request
    ) {
        return SuccessResponse.ok(courseRecommendationService.previewDraftCard(request, currentUser));
    }

    @GetMapping("/previews/{previewId}")
    @Operation(
            summary = "포짓 추천 여행 미리보기 상세 조회",
            description = "포짓 추천 여행 카드 미리보기에서 발급된 previewId로 추천 코스 상세를 조회합니다. 이 시점에는 여행을 DB에 생성하지 않습니다."
    )
    public SuccessResponse<RecommendedCourseResponse> getDraftPreview(
            @CurrentUser User currentUser,
            @Parameter(description = "추천 미리보기 ID") @PathVariable String previewId
    ) {
        return SuccessResponse.ok(courseRecommendationService.getDraftPreview(currentUser, previewId));
    }

    @PostMapping("/start")
    @Operation(
            summary = "포짓 추천 여행 편집 시작",
            description = "여행 시작하기 시점에 previewId와 추천 코스를 바탕으로 편집용 임시 여행과 코스를 생성합니다. 임시 여행은 여행 목록에 노출되지 않습니다."
    )
    public SuccessResponse<TravelCreateResponse> startRecommendedTravel(
            @CurrentUser User currentUser,
            @Valid @RequestBody RecommendedTravelStartRequest request
    ) {
        return SuccessResponse.ok(courseRecommendationService.startRecommendedTravel(currentUser, request));
    }

    @PostMapping("/{travelId}/complete")
    @Operation(
            summary = "포짓 추천 여행 최종 생성",
            description = "코스 편집 완료 시점에 임시 여행을 정식 여행으로 확정합니다. 확정 후 여행 목록에 노출됩니다."
    )
    public SuccessResponse<Void> completeRecommendedTravel(
            @CurrentUser User currentUser,
            @Parameter(description = "임시 여행 ID") @PathVariable Long travelId
    ) {
        courseRecommendationService.completeRecommendedTravel(currentUser, travelId);
        return SuccessResponse.ok();
    }

    @DeleteMapping("/{travelId}")
    @Operation(
            summary = "포짓 추천 임시 여행 삭제",
            description = "추천 여행 코스 편집 중 이탈하거나 취소할 때 임시 여행과 코스를 삭제합니다."
    )
    public SuccessResponse<Void> cancelRecommendedTravel(
            @CurrentUser User currentUser,
            @Parameter(description = "임시 여행 ID") @PathVariable Long travelId
    ) {
        courseRecommendationService.cancelRecommendedTravel(currentUser, travelId);
        return SuccessResponse.ok();
    }
}
