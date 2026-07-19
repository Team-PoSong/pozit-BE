package com.pozit.pozitserver.user.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.dto.request.NotificationSettingRequest;
import com.pozit.pozitserver.user.dto.request.UserUpdateRequest;
import com.pozit.pozitserver.user.dto.response.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API")
public class UserController {

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "사용자 닉네임, 연동된 소셜 계정 정보를 조회합니다.")
    public SuccessResponse<String> getMyInfo(@CurrentUser User user) {
        System.out.println("user id = " + user.getId());
        System.out.println("nickname = " + user.getNickname());
        return SuccessResponse.ok(user.getNickname());
    }

    @PatchMapping("/me")
    @Operation(summary = "내 정보 수정", description = "사용자 닉네임을 수정합니다.")
    public SuccessResponse<Void> updateMyInfo(@RequestBody UserUpdateRequest request) {
        return SuccessResponse.ok();
    }

    @PatchMapping("/me/notification-settings")
    @Operation(summary = "알림 수신 설정 변경", description = "카테고리별 푸시 알림 수신 여부를 설정합니다.")
    public SuccessResponse<Void> updateNotificationSettings(@RequestBody NotificationSettingRequest request) {
        return SuccessResponse.ok();
    }
}
