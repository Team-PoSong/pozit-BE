package com.pozit.pozitserver.user.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.dto.request.NotificationSettingRequest;
import com.pozit.pozitserver.user.dto.request.UserUpdateRequest;
import com.pozit.pozitserver.user.dto.response.UserInfoResponse;
import com.pozit.pozitserver.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API")
public class UserController {

    private final UserService userService;

    @PatchMapping("/nickname")
    @Operation(summary = "첫 회원가입 시 닉네임 설정", description = "현재 로그인한 사용자의 서비스 닉네임을 설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설정 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "이미 사용 중인 닉네임",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "USER400_1",
                                      "message": "이미 사용 중인 닉네임입니다."
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
            )
    })
    public SuccessResponse<String> setNickname(
            @CurrentUser User user,
            @Valid @RequestBody UserUpdateRequest request
    ){
        return SuccessResponse.ok(userService.makeNewNickname(user, request));
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "사용자 닉네임, 연동된 소셜 계정 정보를 조회합니다.")
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
    public SuccessResponse<UserInfoResponse> getMyInfo(@CurrentUser User user) {
        return SuccessResponse.ok(userService.getMyInfo(user));
    }

    @PatchMapping("/me")
    @Operation(summary = "내 정보 수정", description = "사용자 닉네임을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "이미 사용 중인 닉네임",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "USER400_1",
                                      "message": "이미 사용 중인 닉네임입니다."
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
            )
    })
    public SuccessResponse<Void> updateMyInfo(
            @CurrentUser User user,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        userService.makeNewNickname(user, request);
        return SuccessResponse.ok();
    }

    @PatchMapping("/me/notification-settings")
    @Operation(summary = "알림 수신 설정 변경", description = "카테고리별 푸시 알림 수신 여부를 설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
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
    public SuccessResponse<Void> updateNotificationSettings(
            @CurrentUser User user,
            @RequestBody NotificationSettingRequest request
    ) {
        userService.updateNotificationSettings(user, request);
        return SuccessResponse.ok();
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 계정을 탈퇴 처리합니다. 좋아요, 피드백, 포징 영상 정보는 삭제하고, 공유 여행 이력 보호를 위해 사용자 레코드는 삭제하지 않고 비식별화합니다. Redis에 저장된 모든 기기의 Refresh Token도 함께 삭제됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "탈퇴 성공"),
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
    public SuccessResponse<Void> withdraw(
            @CurrentUser User user
    ) {
        userService.withdraw(user);
        return SuccessResponse.ok();
    }
}
