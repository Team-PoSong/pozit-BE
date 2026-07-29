package com.pozit.pozitserver.pozing.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.pozing.dto.request.PozingSaveRequest;
import com.pozit.pozitserver.pozing.dto.response.PozingSaveResponse;
import com.pozit.pozitserver.pozing.service.PozingService;
import com.pozit.pozitserver.travel.dto.response.PresignedUrlResponse;
import com.pozit.pozitserver.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pozing")
@RequiredArgsConstructor
@Tag(name = "Pozing API")
public class PozingController {

    private final PozingService pozingService;

    @PostMapping("/presigned-url")
    @Operation(summary = "포징 타임랩스 업로드 URL 발급", description = "S3 presigned PUT URL을 발급합니다. 클라이언트는 발급받은 URL로 직접 업로드합니다.")
    public SuccessResponse<PresignedUrlResponse> getPozingPresignedUrl(
            @CurrentUser User user,
            @Parameter(description = "포징을 등록할 코스 장소 ID")
            @RequestParam Long courseSpotId
    ){
        return SuccessResponse.ok(pozingService.getPozingPresignedUrl(user,courseSpotId));
    }

    @PostMapping("/save")
    @Operation(summary = "업로드 완료된 포징 저장", description = "S3 업로드 완료 후 포징 URL을 Pozing 엔티티로 저장합니다. 발급받은 presigned url로 영상 저장 완료 후 호출해주세요.")
    public SuccessResponse<PozingSaveResponse> savePozing(
            @CurrentUser User user,
            @Valid @RequestBody PozingSaveRequest request
    ) {
        return SuccessResponse.ok(pozingService.savePozing(user, request));
    }

}
