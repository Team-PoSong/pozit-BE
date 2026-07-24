package com.pozit.pozitserver.tag.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.tag.dto.response.TagResponse;
import com.pozit.pozitserver.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "Tag API", description = "여행 태그 조회 API입니다.")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(
            summary = "태그 목록 조회",
            description = "여행 생성, 수정, 공개 여행 필터링 등에 사용할 수 있는 전체 태그 목록을 조회합니다."
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
                                          "id": 1,
                                          "name": "맛집"
                                        },
                                        {
                                          "id": 2,
                                          "name": "힐링"
                                        }
                                      ]
                                    }
                                    """)
                    )
            )
    })
    public SuccessResponse<List<TagResponse>> getTags(){
        return SuccessResponse.ok(tagService.getTags());
    }
}
