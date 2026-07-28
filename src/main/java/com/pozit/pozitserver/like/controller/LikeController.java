package com.pozit.pozitserver.like.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.like.service.LikeService;
import com.pozit.pozitserver.travel.dto.response.PublicTravelListResponse;
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
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@Tag(name = "Like API")
public class LikeController {

    private final LikeService likeService;

    @GetMapping
    @Operation(summary = "찜 목록 조회", description = "로그인한 사용자가 찜한 여행 목록을 최근 찜한 순으로 조회합니다.")
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
    public SuccessResponse<List<PublicTravelListResponse>> getLikes(@CurrentUser User user) {
        return SuccessResponse.ok(likeService.getLikes(user));
    }

    @PostMapping("/{travelId}")
    @Operation(summary = "찜하기", description = "공개 설정된 완료 여행을 찜 목록에 추가합니다. 본인이 리더/멤버로 참여한 여행은 찜할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "찜 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "이미 찜했거나 본인이 참여한 여행",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "이미 찜한 여행",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "LIKE400_1",
                                                      "message": "이미 찜한 여행입니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "본인이 참여한 여행",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "LIKE400_2",
                                                      "message": "본인이 참여한 여행은 찜할 수 없습니다."
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
    public SuccessResponse<Void> addLike(
            @Parameter(description = "여행 ID") @PathVariable Long travelId,
            @CurrentUser User user) {
        likeService.addLike(travelId, user);
        return SuccessResponse.ok();
    }

    @DeleteMapping("/{travelId}")
    @Operation(summary = "찜 해제", description = "찜 목록에서 여행을 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "찜 해제 성공"),
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
                    responseCode = "404",
                    description = "찜한 적 없는 여행",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "LIKE404_1",
                                      "message": "찜한 여행을 찾을 수 없습니다."
                                    }
                                    """)
                    )
            )
    })
    public SuccessResponse<Void> deleteLike(
            @Parameter(description = "여행 ID") @PathVariable Long travelId,
            @CurrentUser User user) {
        likeService.deleteLike(travelId, user);
        return SuccessResponse.ok();
    }
}
