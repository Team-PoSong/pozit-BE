package com.pozit.pozitserver.global.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping({"/", "/api/health"})
    public SuccessResponse<Map<String, String>> health() {
        return SuccessResponse.ok(Map.of("status", "UP"));
    }
}
