package com.pozit.pozitserver.notification.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.notification.dto.response.NotificationListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification API")
public class NotificationController {

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "여행 초대, 그룹 활동 등 관련 푸시 알림 수신 내역을 조회합니다.")
    public SuccessResponse<List<NotificationListResponse>> getNotifications() {
        return SuccessResponse.ok(null);
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "개별 알림을 읽음 처리합니다.")
    public SuccessResponse<Void> readNotification(
            @Parameter(description = "알림 ID") @PathVariable Long notificationId) {
        return SuccessResponse.ok();
    }
}
