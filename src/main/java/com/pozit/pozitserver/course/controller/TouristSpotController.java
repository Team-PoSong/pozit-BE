package com.pozit.pozitserver.course.controller;

import com.pozit.pozitserver.course.dto.request.CourseSpotRequest;
import com.pozit.pozitserver.course.dto.response.coursespot.CourseSpotSaveResponse;
import com.pozit.pozitserver.course.dto.response.coursespot.HostTouristSpotRankResponse;
import com.pozit.pozitserver.course.dto.response.coursespot.PlaceSearchResponse;
import com.pozit.pozitserver.course.service.TouristSpotService;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course-spots")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tourist Spot API", description = "관광지 검색 및 검색 결과 저장 API입니다.")
public class TouristSpotController {

    private final TouristSpotService touristSpotService;

    @GetMapping("/search")
    @Operation(
            summary = "관광지 검색",
            description = "관광공사 API를 통해 키워드로 관광지를 검색합니다. 무한스크롤 방식으로 사용할 수 있도록 cursor, hasNext, nextCursor를 반환합니다. 검색 결과는 아직 DB에 저장되지 않으며, 사용자가 선택한 장소는 별도 저장 API로 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "검색 성공",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "요청에 성공했습니다.",
                                      "result": {
                                        "currentCursor": 1,
                                        "nextCursor": 2,
                                        "hasNext": true,
                                        "size": 5,
                                        "places": [
                                          {
                                            "contentid": "126508",
                                            "contenttypeid": "12",
                                            "title": "경복궁",
                                            "address": "서울특별시 종로구 사직로 161",
                                            "imageUrl": "https://...",
                                            "longitude": 126.976889,
                                            "latitude": 37.579617
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색어 길이 등 요청값 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON400",
                                      "message": "입력값 검증에 실패했습니다.",
                                      "result": {
                                        "keyword": "크기가 2에서 50 사이여야 합니다"
                                      }
                                    }
                                    """)
                    )
            )
    })
    public SuccessResponse<PlaceSearchResponse> searchCourseSpots(
            @Parameter(description = "검색 키워드. 2자 이상 50자 이하", example = "경복궁")
            @RequestParam @NotBlank @Size(max=50,min=2) String keyword,

            @Parameter(description = "무한스크롤 커서. 첫 요청은 1, 다음 요청부터는 이전 응답의 nextCursor 사용", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) int cursor,

            @Parameter(description = "한 번에 가져올 장소 수", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int size
    ){
        return SuccessResponse.ok(touristSpotService.search(keyword,cursor,size));
    }

    @GetMapping("/ranks")
    @Operation(
            summary = "해당 지역 내의 인기 관광지 랭킹 조회",
            description = "코스에 많이 등록된 관광지를 기준으로 인기 관광지 랭킹을 조회합니다. regionCode를 전달하지 않으면 전체 지역 기준으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "요청에 성공했습니다.",
                                      "result": [
                                        {
                                          "rank": 1,
                                          "touristSpotId": 1,
                                          "title": "경복궁",
                                          "address": "서울특별시 종로구 사직로 161",
                                          "imageUrl": "https://...",
                                          "courseSpotCount": 12
                                        }
                                      ]
                                    }
                                    """)
                    )
            )
    })
    public SuccessResponse<List<HostTouristSpotRankResponse>> getHostTouristSpotsRank(
            @Parameter(description = "지역 코드. 미전달 시 전체 지역 기준", example = "11")
            @RequestParam(required = false) String regionCode,

            @Parameter(description = "조회할 랭킹 개수", example = "3")
            @RequestParam(defaultValue = "3") @Min(1) @Max(20) int limit
    ) {
        return SuccessResponse.ok(touristSpotService.getHostTouristSpotsRank(regionCode, limit));
    }

    @PostMapping
    @Operation(
            summary = "검색 결과 관광지 다건 저장",
            description = "사용자가 검색 결과에서 선택한 장소들의 contentId만 전달하면 TouristSpot으로 저장합니다. 저장 데이터는 직전에 검색한 결과 캐시를 사용하며, 동일한 contentId가 이미 존재하면 새로 저장하지 않고 기존 touristSpotId를 반환합니다. CourseSpot 연결은 기존 코스 수정 API에서 처리합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "검색 결과에서 사용자가 선택한 관광공사 콘텐츠 ID 목록",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = CourseSpotRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "contentIds": [
                                "126508"
                              ]
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "저장 성공 또는 기존 관광지 반환",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "요청에 성공했습니다.",
                                      "result": {
                                        "spots": [
                                          {
                                            "contentId": "126508",
                                            "touristSpotId": 1,
                                            "title": "경복궁",
                                            "address": "서울특별시 종로구 사직로 161"
                                          },
                                          {
                                            "contentId": "127736",
                                            "touristSpotId": 2,
                                            "title": "창덕궁",
                                            "address": "서울특별시 종로구 율곡로 99"
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON400",
                                      "message": "입력값 검증에 실패했습니다.",
                                      "result": {
                                        "contentIds": "비어 있을 수 없습니다"
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
                    description = "선택한 contentId가 검색 결과 캐시에 없음",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COURSE404_1",
                                      "message": "검색 결과에서 선택한 관광지를 찾을 수 없습니다. 다시 검색해주세요."
                                    }
                                    """)
                    )
            )
    })
    public SuccessResponse<CourseSpotSaveResponse> saveSpotsToCourse(
            @RequestBody @Valid CourseSpotRequest request){
        return SuccessResponse.ok(touristSpotService.saveSpotsToCourse(request));
    }
}
