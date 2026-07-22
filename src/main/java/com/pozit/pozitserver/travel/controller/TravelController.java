package com.pozit.pozitserver.travel.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.travel.dto.request.TravelUpdateRequest;
import com.pozit.pozitserver.travel.dto.request.TravelVisibilityRequest;
import com.pozit.pozitserver.travel.dto.response.TravelDetailResponse;
import com.pozit.pozitserver.travel.dto.response.TravelListResponse;
import com.pozit.pozitserver.travel.dto.response.PresignedUrlResponse;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travels")
@RequiredArgsConstructor
@Tag(name = "Travel API")
public class TravelController {

    private final TravelService travelService;

    @GetMapping
    @Operation(summary = "여행 목록 조회", description = "미완료(예정/진행중) 및 완료된 지난 여행 목록을 구분하여 조회합니다.")
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
            )
    })
    public SuccessResponse<List<TravelListResponse>> getTravels(
            @CurrentUser User currentUser,
            @Parameter(description = "완료 여부 (true: 완료, false: 미완료)") @RequestParam(defaultValue = "false") boolean isDone) {
        return SuccessResponse.ok(travelService.getTravels(currentUser, isDone));
    }

    @GetMapping("/{travelId}")
    @Operation(summary = "여행 상세 조회", description = "선택한 여행의 일정, 장소 리스트, 타임랩스, 참여 멤버, 진행률을 조회합니다.")
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
                    description = "존재하지 않는 여행",
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
    public SuccessResponse<TravelDetailResponse> getTravelDetail(
            @CurrentUser User currentUser,
            @Parameter(description = "여행 ID") @PathVariable Long travelId) {
        return SuccessResponse.ok(travelService.getTravelDetail(currentUser, travelId));
    }

    @PatchMapping("/{travelId}")
    @Operation(summary = "여행 정보 수정", description = "리더가 제목, 목적지, 날짜, 태그 등 여행 정보를 수정합니다. 공개 설정은 별도 API로, 배경 사진은 별도 이슈로 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패, 여행 기간 역전 또는 완료된 여행의 날짜 변경 시도",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "여행 기간 역전",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "TRAVEL400_1",
                                                      "message": "종료일은 시작일보다 빠를 수 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "완료된 여행의 날짜 변경 시도",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "TRAVEL400_3",
                                                      "message": "완료된 여행은 날짜를 수정할 수 없습니다."
                                                    }
                                                    """
                                    )
                            }
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
                    description = "존재하지 않는 여행 또는 태그",
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
    public SuccessResponse<Void> updateTravel(
            @CurrentUser User currentUser,
            @Parameter(description = "여행 ID") @PathVariable Long travelId,
            @Valid @RequestBody TravelUpdateRequest request) {
        travelService.updateTravel(currentUser, travelId, request);
        return SuccessResponse.ok();
    }

    @PatchMapping("/{travelId}/visibility")
    @Operation(summary = "여행 공개 설정", description = "리더가 완료된 여행에 한해 공개/비공개를 설정합니다. 기본은 비공개, 공개 시 코스, 완주율, 배경 사진이 공개됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패 또는 완료되지 않은 여행의 공개 설정 변경 시도",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "TRAVEL400_2",
                                      "message": "완료된 여행만 공개 설정을 변경할 수 있습니다."
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
                    description = "존재하지 않는 여행",
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
    public SuccessResponse<Void> updateVisibility(
            @CurrentUser User currentUser,
            @Parameter(description = "여행 ID") @PathVariable Long travelId,
            @Valid @RequestBody TravelVisibilityRequest request) {
        travelService.updateVisibility(currentUser, travelId, request);
        return SuccessResponse.ok();
    }

    @PostMapping("/{travelId}/background-image")
    @Operation(summary = "배경 사진 업로드 URL 발급", description = "S3 presigned URL을 발급합니다. 클라이언트는 해당 URL로 직접 업로드합니다.")
    public SuccessResponse<PresignedUrlResponse> getBackgroundImageUploadUrl(
            @Parameter(description = "여행 ID") @PathVariable Long travelId) {
        return SuccessResponse.ok(null);
    }
}
