package com.pozit.pozitserver.travel.controller;

import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.travel.dto.response.RegionSearchResponse;
import com.pozit.pozitserver.travel.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions")
@Tag(name = "Region API", description = "여행 지역 검색 API입니다.")
public class RegionController {

    private final RegionService regionService;

    @GetMapping("/search")
    @Operation(
            summary = "여행 지역 검색",
            description = "키워드로 여행 지역을 검색합니다. 공백 또는 빈 키워드는 빈 목록을 반환하며, 최대 10개까지 조회됩니다."
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
                                      "result": [
                                        {
                                          "code": "11000",
                                          "name": "서울특별시",
                                          "provinceCode": "11000",
                                          "provinceName": "서울특별시"
                                        }
                                      ]
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "keyword 요청 파라미터 누락",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON400",
                                      "message": "잘못된 요청입니다."
                                    }
                                    """)
                    )
            )
    })
    public SuccessResponse<List<RegionSearchResponse>> getRegions(
            @Parameter(description = "검색할 지역 키워드", example = "서울")
            @RequestParam String keyword
    ){
        return SuccessResponse.ok(regionService.searchRegions(keyword));
    }

}
