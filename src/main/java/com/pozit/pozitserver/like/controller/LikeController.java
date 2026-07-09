package com.pozit.pozitserver.like.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.like.dto.response.LikeListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@Tag(name = "Like API")
public class LikeController {

    @GetMapping
    @Operation(summary = "찜 목록 조회", description = "내가 찜을 누른 공개 여행 코스 목록을 조회합니다.")
    public SuccessResponse<List<LikeListResponse>> getWishes(
            @Parameter(description = "태그 ID 필터") @RequestParam(required = false) Long tagId,
            @Parameter(description = "지역 필터") @RequestParam(required = false) String destination) {
        return SuccessResponse.ok(null);
    }

    @PostMapping("/{travelId}")
    @Operation(summary = "찜하기", description = "공개 여행 코스를 찜 목록에 추가(하트)합니다.")
    public SuccessResponse<Void> addWish(
            @Parameter(description = "여행 ID") @PathVariable Long travelId) {
        return SuccessResponse.ok();
    }

    @DeleteMapping("/{travelId}")
    @Operation(summary = "찜 해제", description = "찜 목록에서 여행 코스를 제거합니다.")
    public SuccessResponse<Void> deleteWish(
            @Parameter(description = "여행 ID") @PathVariable Long travelId) {
        return SuccessResponse.ok();
    }
}
