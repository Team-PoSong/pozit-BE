package com.pozit.pozitserver.test.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.test.dto.SampleResponseDto;
import com.pozit.pozitserver.test.service.TestService;
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

    private final TestService testService;

    @GetMapping("")
    @Operation(summary = "테스트용",description = "테스트용 API입니다.")
    public SuccessResponse<SampleResponseDto> test(){
        return SuccessResponse.ok(testService.getSample());
    }
}
