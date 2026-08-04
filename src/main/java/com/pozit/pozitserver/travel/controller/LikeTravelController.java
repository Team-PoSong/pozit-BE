package com.pozit.pozitserver.travel.controller;

import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.travel.dto.request.LikeBasedTravelCreateRequest;
import com.pozit.pozitserver.travel.dto.response.LikeBasedTravelDraftResponse;
import com.pozit.pozitserver.travel.dto.response.TravelCreateResponse;
import com.pozit.pozitserver.travel.service.LikeTravelService;
import com.pozit.pozitserver.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/like-based")
@RequiredArgsConstructor
@Tag(name = "Like Based Travel API")
public class LikeTravelController {

    private final LikeTravelService likeTravelService;

    @GetMapping("/travels/{travelId}/draft")
    @Operation(
            summary = "찜 기반 여행 생성 초안 조회",
            description = "공개 여행을 기반으로 생성 화면에 사용할 초안을 조회합니다. 이 API는 DB에 새 여행을 생성하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "초안 조회 성공",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "요청에 성공했습니다.",
                                      "result": {
                                        "sourceTravelId": 1,
                                        "title": "서울 감성 여행",
                                        "destination": "서울특별시",
                                        "regionCode": "11000",
                                        "startDate": "2026-08-01",
                                        "endDate": "2026-08-03",
                                        "transportation": "PUBLIC",
                                        "travelStyle": "RELAXED",
                                        "backgroundImageUrl": "https://...",
                                        "tagIds": [1, 2],
                                        "courses": [
                                          {
                                            "sourceCourseId": 1,
                                            "dayNumber": 1,
                                            "date": "2026-08-01",
                                            "spots": [
                                              {
                                                "sourceCourseSpotId": 1,
                                                "touristSpotId": 10,
                                                "title": "경복궁",
                                                "address": "서울특별시 종로구 사직로 161",
                                                "imageUrl": "https://...",
                                                "orderIndex": 1
                                              }
                                            ]
                                          }
                                        ]
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
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않거나 공개되지 않은 여행",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "TRAVEL404_1",
                                      "message": "해당 여행을 찾을 수 없습니다."
                                    }
                                    """)
                    )
            )
    })
    public SuccessResponse<LikeBasedTravelDraftResponse> getLikeBasedTravelDraft(
            @Parameter(description = "복사할 원본 여행 ID") @PathVariable Long travelId
    ) {
        return SuccessResponse.ok(likeTravelService.getLikeBasedTravelDraft(travelId));
    }

    @PostMapping("/travels")
    @Operation(
            summary = "찜 기반 여행 최종 생성",
            description = "초안 조회 후 사용자가 수정한 최종 값으로 새 여행을 생성합니다. 초대코드와 여행 멤버는 새로 생성하며, 호출한 사용자가 새 여행의 리더가 됩니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "찜 기반 여행 최종 생성 요청",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = LikeBasedTravelCreateRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "sourceTravelId": 1,
                              "title": "내가 가는 서울 여행",
                              "startDate": "2026-08-10",
                              "endDate": "2026-08-12",
                              "transportation": "PUBLIC",
                              "travelStyle": "RELAXED",
                              "backgroundImageUrl": "https://...",
                              "tagIds": [1, 2],
                              "courses": [
                                {
                                  "dayNumber": 1,
                                  "touristSpotIds": [10, 11, 12]
                                }
                              ]
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "여행 생성 성공",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "요청에 성공했습니다.",
                                      "result": {
                                        "travelId": 10,
                                        "courses": [
                                          {
                                            "courseId": 21,
                                            "dayNumber": 1,
                                            "date": "2026-08-10"
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            )
    })
    public SuccessResponse<TravelCreateResponse> createTravelFromLikeDraft(
            @CurrentUser User user,
            @Valid @RequestBody LikeBasedTravelCreateRequest request
    ) {
        return SuccessResponse.ok(likeTravelService.createTravelFromLikeDraft(user, request));
    }

}
