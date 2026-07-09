package com.pozit.pozitserver.support.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.support.dto.request.FeedbackRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "Support API")
public class SupportController {

    @GetMapping("/info")
    @Operation(summary = "서비스 안내 조회", description = "이용약관 및 개인정보 처리방침을 조회합니다.")
    public SuccessResponse<Void> getServiceInfo() {
        return SuccessResponse.ok();
    }

    @PostMapping("/feedback")
    @Operation(summary = "피드백 전송", description = "고객 피드백을 전송합니다.")
    public SuccessResponse<Void> sendFeedback(@RequestBody FeedbackRequest request) {
        return SuccessResponse.ok();
    }
}
