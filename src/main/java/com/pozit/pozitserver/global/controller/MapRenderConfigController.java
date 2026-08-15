package com.pozit.pozitserver.global.controller;

import com.pozit.pozitserver.global.response.SuccessResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/map-render")
public class MapRenderConfigController {

    private final String kakaoJavaScriptKey;

    public MapRenderConfigController(
            @Value("${kakao.map.javascript-key:}") String kakaoJavaScriptKey
    ) {
        this.kakaoJavaScriptKey = kakaoJavaScriptKey;
    }

    @GetMapping("/config")
    public SuccessResponse<MapRenderConfigResponse> getConfig() {
        return SuccessResponse.ok(new MapRenderConfigResponse(kakaoJavaScriptKey));
    }

    public record MapRenderConfigResponse(
            String kakaoJavaScriptKey
    ) {
    }
}
