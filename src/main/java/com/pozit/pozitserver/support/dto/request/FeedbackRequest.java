package com.pozit.pozitserver.support.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "고객 피드백 전송 요청")
public record FeedbackRequest(
        @Schema(description = "피드백 내용", example = "앱이 자주 튕겨요.", maxLength = 1000)
        @NotBlank(message = "피드백 내용을 입력해주세요.")
        @Size(max = 1000, message = "피드백 내용은 최대 1000자까지 가능합니다.")
        String content
) {}
