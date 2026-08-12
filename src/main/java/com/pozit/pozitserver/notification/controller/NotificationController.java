package com.pozit.pozitserver.notification.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.notification.dto.response.NotificationListResponse;
import com.pozit.pozitserver.notification.dto.response.NotificationUnreadCountResponse;
import com.pozit.pozitserver.notification.service.NotificationService;
import com.pozit.pozitserver.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "여행 초대, 그룹 활동 등 관련 푸시 알림 수신 내역을 조회합니다. 조회된 알림은 조회 즉시 모두 읽음 처리됩니다.")
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
    public SuccessResponse<List<NotificationListResponse>> getNotifications(@CurrentUser User user) {
        return SuccessResponse.ok(notificationService.getNotifications(user));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "안읽은 알림 개수 조회", description = "읽음 처리를 발생시키지 않는 순수 조회 API입니다.")
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
    public SuccessResponse<NotificationUnreadCountResponse> getUnreadCount(@CurrentUser User user) {
        return SuccessResponse.ok(notificationService.getUnreadCount(user));
    }
}
