package com.pozit.pozitserver.pozing.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.pozing.dto.request.PozingSaveRequest;
import com.pozit.pozitserver.pozing.dto.response.PozingEditJobCreateResponse;
import com.pozit.pozitserver.pozing.dto.response.PozingEditJobStatusResponse;
import com.pozit.pozitserver.pozing.dto.response.PozingPresignedUrlResponse;
import com.pozit.pozitserver.pozing.dto.response.PozingSaveResponse;
import com.pozit.pozitserver.pozing.dto.response.PozingThumbnailStatusResponse;
import com.pozit.pozitserver.pozing.service.PozingService;
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
    public SuccessResponse<PozingPresignedUrlResponse> getPozingPresignedUrl(
            @CurrentUser User user,
            @Parameter(description = "포징을 등록할 코스 장소 ID")
            @RequestParam Long courseSpotId
    ){
        return SuccessResponse.ok(pozingService.getPozingPresignedUrl(user,courseSpotId));
    }

    @PostMapping("/save")
    @Operation(summary = "업로드 완료된 포징 저장", description = "S3 업로드 완료 후 presigned URL 발급 응답의 objectKey를 전달해 Pozing 엔티티로 저장합니다.")
    public SuccessResponse<PozingSaveResponse> savePozing(
            @CurrentUser User user,
            @Valid @RequestBody PozingSaveRequest request
    ) {
        return SuccessResponse.ok(pozingService.savePozing(user, request));
    }

    @PostMapping("/local-save")
    @Operation(summary = "포징 영상 편집 요청",description = "S3에 업로드된 해당 여행의 영상들에 대한 편집 요청을 합니다.")
    public SuccessResponse<PozingEditJobCreateResponse> requestEditPozing(
            @CurrentUser User user,
            @Parameter(description = "편집할 여행 id")
            @RequestParam Long travelId
    ){
        return SuccessResponse.ok(pozingService.requestEditPozing(user,travelId));
    }

    @GetMapping("/edit-jobs/{jobId}")
    @Operation(summary = "포징 영상 편집 작업 상태 조회", description = "편집 작업 상태를 조회합니다. 완료 상태이면 임시 S3 다운로드 URL을 함께 반환합니다.")
    public SuccessResponse<PozingEditJobStatusResponse> getEditPozingJob(
            @CurrentUser User user,
            @PathVariable Long jobId
    ) {
        return SuccessResponse.ok(pozingService.getEditPozingJob(user, jobId));
    }

    @GetMapping("/{pozingId}/thumbnail")
    @Operation(summary = "포징 썸네일 생성 상태 조회", description = "포징 썸네일 생성 상태를 조회합니다. 완료 상태이면 임시 S3 썸네일 URL을 함께 반환합니다.")
    public SuccessResponse<PozingThumbnailStatusResponse> getThumbnailStatus(
            @CurrentUser User user,
            @Parameter(description = "포징 ID") @PathVariable Long pozingId
    ) {
        return SuccessResponse.ok(pozingService.getThumbnailStatus(user, pozingId));
    }

}
