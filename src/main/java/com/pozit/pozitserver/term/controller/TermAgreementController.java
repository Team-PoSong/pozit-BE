package com.pozit.pozitserver.term.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.term.dto.request.TermAgreementRequest;
import com.pozit.pozitserver.term.dto.response.TermAgreementResponse;
import com.pozit.pozitserver.term.service.TermAgreementService;
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
@RequestMapping("/api/terms/agreements")
@RequiredArgsConstructor
@Tag(name = "Term Agreement API")
public class TermAgreementController {

    private final TermAgreementService termAgreementService;

    @PostMapping
    @Operation(
            summary = "약관 동의 저장",
            description = """
                    약관 종류(termType)별 동의 여부를 목록으로 받아 저장합니다.
                    약관 종류/필수 여부가 아직 확정 전이라 termType은 자유 문자열이며, 필수 항목 검증은 현재 서버에서 강제하지 않습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 형식 오류 (termType 누락, agreed 누락 등)",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON400",
                                      "message": "입력값 검증에 실패했습니다.",
                                      "errors": {
                                        "agreements[0].termType": "약관 종류는 필수입니다."
                                      }
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
    public SuccessResponse<TermAgreementResponse> agree(
            @CurrentUser User user,
            @Valid @RequestBody TermAgreementRequest request
    ) {
        return SuccessResponse.ok(termAgreementService.agree(user, request));
    }

    @GetMapping
    @Operation(summary = "약관 동의 내역 조회", description = "로그인한 사용자의 약관 동의 여부와 동의 시점을 조회합니다. 저장된 동의 기록이 없으면 빈 목록을 반환합니다.")
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
    public SuccessResponse<TermAgreementResponse> getAgreement(@CurrentUser User user) {
        return SuccessResponse.ok(termAgreementService.getAgreement(user));
    }
}
