package com.pozit.pozitserver.test.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public SuccessResponse<String> test(){
        return SuccessResponse.ok("테스트 성공");
    }
}
