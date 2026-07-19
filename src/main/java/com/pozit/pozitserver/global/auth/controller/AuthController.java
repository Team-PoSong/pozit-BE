package com.pozit.pozitserver.global.auth.controller;

import com.pozit.pozitserver.global.auth.dto.LoginTokenResponse;
import com.pozit.pozitserver.global.auth.kakao.KakaoProperties;
import com.pozit.pozitserver.global.auth.service.AuthService;
import com.pozit.pozitserver.global.response.SuccessResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoProperties kakaoProperties;
    private final AuthService authService;

    @GetMapping("/kakao")
    public void redirectToKakao(
            HttpServletResponse response
    ) throws IOException {

        String authorizationUrl =
                UriComponentsBuilder
                        .fromUriString("https://kauth.kakao.com/oauth/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", kakaoProperties.clientId())
                        .queryParam("redirect_uri", kakaoProperties.redirectUri())
                        .build()
                        .encode()
                        .toUriString();

        response.sendRedirect(authorizationUrl);
    }

    @GetMapping("/kakao/callback")
    public SuccessResponse<LoginTokenResponse> kakaoCallback(
            @RequestParam String code
    ) {
        LoginTokenResponse response = authService.loginWithKakao(code);
        return SuccessResponse.ok(response);
    }
}
