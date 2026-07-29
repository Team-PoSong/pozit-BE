package com.pozit.pozitserver.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 정보 수정 요청")
public record UserUpdateRequest(
        @Schema(description = "서비스에서 사용할 닉네임", example = "민서", minLength = 1, maxLength = 5)
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(max = 5, message = "닉네임은 최대 5자까지 가능합니다.")
        String nickname
) {}
