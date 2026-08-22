package com.pozit.pozitserver.travel.controller;

import com.pozit.pozitserver.course.dto.response.CourseDetailResponse;
import com.pozit.pozitserver.course.service.CourseService;
import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.travel.dto.response.PublicTravelDetailResponse;
import com.pozit.pozitserver.travel.dto.response.PublicTravelListResponse;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public Travel API")
public class PublicTravelController {

    private final TravelService travelService;
    private final CourseService courseService;

    @GetMapping("/travels")
    @Operation(summary = "공개 여행 피드 조회", description = "공개 설정된 완료 여행 목록을 지역/기간/태그/키워드로 검색·필터링하여 조회합니다. 로그인한 사용자의 경우 본인이 리더 또는 멤버로 참여한 여행은 제외됩니다. 비로그인 상태에서도 조회할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색 조건 오류 (startDate/endDate 중 하나만 전달했거나, startDate가 endDate보다 늦은 경우)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "날짜 한쪽만 전달",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "TRAVEL400_8",
                                                      "message": "검색 기간이 올바르지 않습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "시작일이 종료일 이후",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "TRAVEL400_1",
                                                      "message": "종료일은 시작일보다 빠를 수 없습니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    public SuccessResponse<List<PublicTravelListResponse>> getPublicTravels(
            @CurrentUser(required = false) User currentUser,
            @Parameter(description = "지역 코드 (시/도 단위 접두사 매칭, 미전달 시 전체 지역)") @RequestParam(required = false) String regionCode,
            @Parameter(description = "여행 기간 시작일 (endDate와 함께 있어야 적용)") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "여행 기간 종료일 (startDate와 함께 있어야 적용)") @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "태그 ID 목록 (모두 포함하는 여행 조회)") @RequestParam(required = false) List<Long> tagIds,
            @Parameter(description = "키워드 (여행명/지역명/코스 관광지명 부분 일치)") @RequestParam(required = false) String keyword) {
        return SuccessResponse.ok(travelService.getPublicTravels(currentUser, regionCode, startDate, endDate, tagIds, keyword));
    }

    @GetMapping("/travels/popular/cards")
    @Operation(summary = "인기 공개 여행 카드 조회", description = "지역과 상관없이 공개 설정된 완료 여행 중 좋아요 수가 많은 순서로 최대 3개를 카드 형태로 조회합니다. 로그인한 사용자의 경우 본인이 리더 또는 멤버로 참여한 여행은 제외됩니다.")
    public SuccessResponse<List<PublicTravelListResponse>> getPopularPublicTravelCards(
            @CurrentUser(required = false) User currentUser
    ) {
        return SuccessResponse.ok(travelService.getPopularPublicTravelCards(currentUser));
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
            @CurrentUser(required = false) User currentUser,
            @Parameter(description = "여행 ID") @PathVariable Long travelId) {
        return SuccessResponse.ok(travelService.getPublicTravelDetail(travelId, currentUser));
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
