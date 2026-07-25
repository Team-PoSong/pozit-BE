package com.pozit.pozitserver.travel.controller;

import com.pozit.pozitserver.course.dto.response.CourseDetailResponse;
import com.pozit.pozitserver.course.service.CourseService;
import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.travel.dto.response.PublicTravelDetailResponse;
import com.pozit.pozitserver.travel.dto.response.TravelListResponse;
import com.pozit.pozitserver.travel.service.TravelService;
import com.pozit.pozitserver.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public Travel API")
public class PublicTravelController {

    private final TravelService travelService;
    private final CourseService courseService;

    @GetMapping("/travels")
    @Operation(summary = "공개 여행 피드 조회", description = "공개 설정된 완료 여행 목록을 조회합니다. 로그인한 사용자의 경우 본인이 리더 또는 멤버로 참여한 여행은 제외됩니다. 비로그인 상태에서도 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public SuccessResponse<List<TravelListResponse>> getPublicTravels(
            @CurrentUser(required = false) User currentUser) {
        return SuccessResponse.ok(travelService.getPublicTravels(currentUser));
    }

    @GetMapping("/travels/{travelId}")
    @Operation(summary = "공개 여행 상세 조회", description = "공개 설정된 완료 여행의 상세 정보를 조회합니다. 비로그인 상태에서도 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않거나 공개되지 않은 여행",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "TRAVEL404_1",
                                      "message": "해당 여행을 찾을 수 없습니다."
                                    }
                                    """)
                    )
            )
    })
    public SuccessResponse<PublicTravelDetailResponse> getPublicTravelDetail(
            @Parameter(description = "여행 ID") @PathVariable Long travelId) {
        return SuccessResponse.ok(travelService.getPublicTravelDetail(travelId));
    }

    @GetMapping("/courses/{courseId}")
    @Operation(summary = "공개 여행코스 상세 조회", description = "공개 설정된 완료 여행에 속한 코스를 상세 조회합니다. 비로그인 상태에서도 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 코스이거나 코스가 속한 여행이 공개되지 않음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON404",
                                      "message": "요청한 리소스를 찾을 수 없습니다."
                                    }
                                    """)
                    )
            )
    })
    public SuccessResponse<CourseDetailResponse> getPublicCourseDetail(
            @Parameter(description = "코스 ID") @PathVariable Long courseId) {
        return SuccessResponse.ok(courseService.getPublicCourseDetail(courseId));
    }
}
