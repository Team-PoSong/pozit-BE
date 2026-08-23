package com.pozit.pozitserver.support.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.support.dto.request.FeedbackRequest;
import com.pozit.pozitserver.support.dto.response.SupportInfoResponse;
import com.pozit.pozitserver.support.service.SupportService;
import com.pozit.pozitserver.user.domain.User;
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
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "Support API")
public class SupportController {

    private final SupportService supportService;

    @GetMapping("/info")
    @Operation(summary = "서비스 안내 조회", description = "이용약관, 개인정보 처리방침 및 위치기반서비스 이용약관의 최신 버전을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "약관 정보를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "TERM404_1",
                                      "message": "약관 정보를 찾을 수 없습니다."
                                    }
                                    """)
                    )
            )
    })
    public SuccessResponse<SupportInfoResponse> getServiceInfo() {
        return SuccessResponse.ok(supportService.getSupportInfo());
    }

    @PostMapping("/feedback")
    @Operation(summary = "피드백 전송", description = "로그인한 사용자의 고객 피드백을 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전송 성공"),
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
    public SuccessResponse<Void> sendFeedback(
            @CurrentUser User user,
            @Valid @RequestBody FeedbackRequest request
    ) {
        supportService.saveFeedback(user, request);
        return SuccessResponse.ok();
    }
}
