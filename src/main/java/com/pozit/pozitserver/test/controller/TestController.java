package com.pozit.pozitserver.test.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name="test API",description = "test API입니다.")
public class TestController {

    @GetMapping("")
    @Operation(summary = "테스트용",description = "테스트용 API입니다.")
    public SuccessResponse<String> test(){
        return SuccessResponse.ok("테스트 성공");
    }
}
