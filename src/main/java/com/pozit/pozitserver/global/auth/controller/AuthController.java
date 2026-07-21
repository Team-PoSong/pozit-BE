package com.pozit.pozitserver.global.auth.controller;

import com.pozit.pozitserver.global.auth.dto.KakaoAccessTokenRequest;
import com.pozit.pozitserver.global.auth.dto.LoginTokenResponse;
import com.pozit.pozitserver.global.auth.kakao.KakaoProperties;
import com.pozit.pozitserver.global.auth.service.AuthService;
import com.pozit.pozitserver.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name="카카오 로그인 API",description = "카카오 로그인 관련 API 입니다.")
public class AuthController {

    private final KakaoProperties kakaoProperties;
    private final AuthService authService;

    @Hidden
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

    @Hidden
    @GetMapping("/kakao/callback")
    public SuccessResponse<LoginTokenResponse> kakaoCallback(
            @RequestParam String code
    ) {
        LoginTokenResponse response = authService.loginWithKakao(code);
        return SuccessResponse.ok(response);
    }

    @Operation(
            summary = "카카오 네이티브 앱 로그인",
            description = "Flutter Kakao SDK에서 발급받은 카카오 accessToken을 전달받아 카카오 사용자 정보를 조회하고, 회원 조회 또는 가입 후 POZIT JWT를 발급합니다. Native App Key, REST API Key, refreshToken, idToken, POZIT JWT가 아니라 OAuthToken.accessToken 값만 전달해야 합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Flutter Kakao SDK 로그인 성공 결과로 받은 OAuthToken.accessToken",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = KakaoAccessTokenRequest.class),
                    examples = @ExampleObject(
                            name = "카카오 accessToken 요청",
                            value = """
                                    {
                                      "accessToken": "kakao_access_token"
                                    }
                                    """
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공 및 POZIT JWT 발급"),
            @ApiResponse(responseCode = "400", description = "accessToken 누락 또는 빈 값"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 카카오 액세스 토큰")
    })
    @PostMapping("/kakao/native")
    public SuccessResponse<LoginTokenResponse> kakaoNativeLogin(
            @Valid @RequestBody KakaoAccessTokenRequest request
    ) {
        LoginTokenResponse response = authService.loginWithKakaoAccessToken(request.accessToken());
        return SuccessResponse.ok(response);
    }
}
