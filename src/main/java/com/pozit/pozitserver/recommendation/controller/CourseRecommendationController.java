package com.pozit.pozitserver.recommendation.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.recommendation.dto.RecommendedCourseResponse;
import com.pozit.pozitserver.recommendation.service.CourseRecommendationService;
import com.pozit.pozitserver.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/travels/{travelId}/recommendations")
@RequiredArgsConstructor
@Tag(name = "Course Recommendation API")
public class CourseRecommendationController {

    private final CourseRecommendationService courseRecommendationService;

    @PostMapping("/preview")
    @Operation(
            summary = "여행 코스 추천 미리보기",
            description = "여행 조건과 관광정보 API 후보를 기반으로 날짜별 추천 코스를 생성합니다. 추천 결과는 DB에 저장하지 않습니다."
    )
    public SuccessResponse<RecommendedCourseResponse> preview(
            @CurrentUser User currentUser,
            @Parameter(description = "여행 ID") @PathVariable Long travelId
    ) {
        return SuccessResponse.ok(courseRecommendationService.preview(travelId, currentUser));
    }
}
