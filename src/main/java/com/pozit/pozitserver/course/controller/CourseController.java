package com.pozit.pozitserver.course.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.course.dto.request.CourseSpotUpdateRequest;
import com.pozit.pozitserver.course.dto.request.LocationRequest;
import com.pozit.pozitserver.course.dto.response.CourseDetailResponse;
import com.pozit.pozitserver.course.service.CourseService;
import com.pozit.pozitserver.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Course API")
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/{courseId}")
    @Operation(summary = "코스 상세 조회", description = "일자별 코스의 장소 리스트와 방문 상태, 장소별 사진을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON401",
                                      "message": "인증되지 않은 요청입니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 여행의 멤버가 아니어서 접근 권한이 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON403",
                                      "message": "접근 권한이 없습니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 코스",
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
    public SuccessResponse<CourseDetailResponse> getCourseDetail(
            @CurrentUser User currentUser,
            @Parameter(description = "코스 ID") @PathVariable Long courseId) {
        return SuccessResponse.ok(courseService.getCourseDetail(currentUser, courseId));
    }

    @PatchMapping("/{courseId}/spots")
    @Operation(summary = "코스 편집 및 저장", description = "리더가 코스의 수정된 장소 목록을 전달하여 추가/삭제/순서 변경을 한 번에 반영합니다. 삭제된 장소의 사진 데이터도 DB에서 함께 삭제됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "완료된 여행의 코스 수정 시도",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "TRAVEL400_4",
                                      "message": "완료된 여행의 코스는 수정할 수 없습니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON401",
                                      "message": "인증되지 않은 요청입니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "여행 리더가 아니어서 수정 권한이 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON403",
                                      "message": "접근 권한이 없습니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 코스 또는 관광지",
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
    public SuccessResponse<Void> updateCourseSpots(
            @CurrentUser User currentUser,
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @Valid @RequestBody CourseSpotUpdateRequest request) {
        courseService.updateCourseSpots(currentUser, courseId, request);
        return SuccessResponse.ok();
    }

    @PatchMapping("/{courseId}/current-location")
    @Operation(summary = "현재 위치 갱신", description = "사용자 현재 GPS 위치를 갱신하고, 관광지 방문 상태를 전이합니다.")
    public SuccessResponse<Void> updateLocation(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @RequestBody LocationRequest request) {
        return SuccessResponse.ok();
    }

}
