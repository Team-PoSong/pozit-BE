package com.pozit.pozitserver.recommendation.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.recommendation.dto.CourseChatRequest;
import com.pozit.pozitserver.recommendation.dto.CourseChatResponse;
import com.pozit.pozitserver.recommendation.dto.RecommendedCourseCardResponse;
import com.pozit.pozitserver.recommendation.dto.RecommendedCourseResponse;
import com.pozit.pozitserver.recommendation.dto.RecommendedCourseSaveRequest;
import com.pozit.pozitserver.recommendation.service.CourseChatService;
import com.pozit.pozitserver.recommendation.service.CourseChatStreamService;
import com.pozit.pozitserver.recommendation.service.CourseRecommendationService;
import com.pozit.pozitserver.user.domain.User;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/travels/{travelId}/recommendations")
@RequiredArgsConstructor
@Tag(name = "Course Recommendation API")
public class CourseRecommendationController {

    private final CourseRecommendationService courseRecommendationService;
    private final CourseChatService courseChatService;
    private final CourseChatStreamService courseChatStreamService;

    @PostMapping("/preview")
    @Hidden
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

    @PostMapping("/preview/card")
    @Operation(
            summary = "여행 코스 추천 카드 미리보기",
            description = "추천 코스를 여행 카드 UI로 바로 표시할 수 있도록 카드 메타 정보와 추천 코스 원본을 함께 반환합니다. 추천 결과는 DB에 저장하지 않습니다."
    )
    public SuccessResponse<RecommendedCourseCardResponse> previewCard(
            @CurrentUser User currentUser,
            @Parameter(description = "여행 ID") @PathVariable Long travelId
    ) {
        return SuccessResponse.ok(courseRecommendationService.previewCard(travelId, currentUser));
    }

    @GetMapping("/previews/{previewId}")
    @Operation(
            summary = "여행 코스 추천 미리보기 상세 조회",
            description = "추천 카드 미리보기에서 발급된 previewId로 날짜별 추천 코스 상세를 조회합니다. 미리보기 데이터는 일정 시간 후 만료됩니다."
    )
    public SuccessResponse<RecommendedCourseResponse> getPreview(
            @CurrentUser User currentUser,
            @Parameter(description = "여행 ID") @PathVariable Long travelId,
            @Parameter(description = "추천 미리보기 ID") @PathVariable String previewId
    ) {
        return SuccessResponse.ok(courseRecommendationService.getPreview(travelId, currentUser, previewId));
    }

    @PostMapping("/commit")
    @Operation(
            summary = "추천 코스 저장",
            description = "사용자가 선택한 추천 코스를 실제 날짜별 코스 장소로 저장합니다. 기존 코스 장소는 추천 결과로 교체됩니다."
    )
    public SuccessResponse<Void> commit(
            @CurrentUser User currentUser,
            @Parameter(description = "여행 ID") @PathVariable Long travelId,
            @Valid @RequestBody RecommendedCourseSaveRequest request
    ) {
        courseRecommendationService.commit(travelId, currentUser, request);
        return SuccessResponse.ok();
    }

    @PostMapping("/chat")
    @Operation(
            summary = "LLM 코스 수정 제안",
            description = "저장된 코스와 사용자 메시지를 기반으로 LLM이 수정 의도를 추출하고, 관광정보 API 후보를 활용해 적용 가능한 코스 수정안을 반환합니다. DB의 코스 장소는 변경하지 않습니다."
    )
    public SuccessResponse<CourseChatResponse> chat(
            @CurrentUser User currentUser,
            @Parameter(description = "여행 ID") @PathVariable Long travelId,
            @Valid @RequestBody CourseChatRequest request
    ) {
        return SuccessResponse.ok(courseChatService.suggest(travelId, currentUser, request));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "LLM 코스 수정 제안 스트리밍",
            description = "챗봇 응답 문장을 SSE message 이벤트로 한 글자씩 전송하고, 마지막 result 이벤트로 적용 가능한 전체 수정안을 반환합니다."
    )
    public SseEmitter chatStream(
            @CurrentUser User currentUser,
            @Parameter(description = "여행 ID") @PathVariable Long travelId,
            @Valid @RequestBody CourseChatRequest request
    ) {
        return courseChatStreamService.stream(travelId, currentUser, request);
    }
}
