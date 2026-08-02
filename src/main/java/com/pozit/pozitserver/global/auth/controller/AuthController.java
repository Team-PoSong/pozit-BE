package com.pozit.pozitserver.global.auth.controller;

import com.pozit.pozitserver.global.auth.dto.request.KakaoAccessTokenRequest;
import com.pozit.pozitserver.global.auth.dto.request.LogoutRequest;
import com.pozit.pozitserver.global.auth.dto.request.TokenReissueRequest;
import com.pozit.pozitserver.global.auth.dto.response.LoginTokenResponse;
import com.pozit.pozitserver.global.auth.annotation.CurrentUser;
import com.pozit.pozitserver.global.auth.ios.AppleIdentityTokenRequest;
import com.pozit.pozitserver.global.auth.kakao.KakaoProperties;
import com.pozit.pozitserver.global.auth.service.AuthAppleService;
import com.pozit.pozitserver.global.auth.service.AuthKakaoService;
import com.pozit.pozitserver.global.auth.service.AuthTokenService;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
import com.pozit.pozitserver.user.domain.User;
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
@Tag(name="소셜 로그인 API",description = "소셜 로그인 관련 API 입니다.")
public class AuthController {

    private final KakaoProperties kakaoProperties;
    private final AuthKakaoService authService;
    private final AuthAppleService authAppleService;
    private final AuthTokenService authTokenService;

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

    @Operation(
            summary = "카카오 네이티브 앱 로그인",
            description = "Flutter Kakao SDK에서 발급받은 카카오 accessToken을 전달받아 카카오 사용자 정보를 조회하고, 회원 조회 또는 가입 후 POZIT JWT를 발급합니다. deviceId는 카카오에서 받는 값이 아니라 앱 클라이언트가 생성한 UUID이며, secure storage 등에 저장하고 같은 기기의 로그인/재발급/로그아웃 요청에서 동일하게 전달해야 합니다."
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
                                      "accessToken": "kakao_access_token",
                                      "deviceId": "550e8400-e29b-41d4-a716-446655440000"
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
        LoginTokenResponse response =
                authService.loginWithKakaoAccessToken(request.accessToken(), request.deviceId());
        return SuccessResponse.ok(response);
    }

    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token과 로그인 시 사용한 deviceId로 새 Access Token과 Refresh Token을 발급합니다. deviceId는 소셜 제공자 값이 아니라 앱 클라이언트가 생성해 보관하는 UUID입니다. 재발급 성공 시 기존 Refresh Token은 폐기되고 새 Refresh Token으로 교체됩니다."
    )
    @PostMapping("/reissue")
    public SuccessResponse<LoginTokenResponse> reissue(
            @Valid @RequestBody TokenReissueRequest request
    ) {
        LoginTokenResponse response =
                authTokenService.reissue(request.refreshToken(), request.deviceId());
        return SuccessResponse.ok(response);
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 로그인한 사용자와 요청 deviceId에 해당하는 기기의 Refresh Token을 Redis에서 삭제합니다. deviceId는 앱 클라이언트가 생성해 보관하는 UUID이며, 카카오/Apple에서 발급받는 값이 아닙니다."
    )
    @PostMapping("/logout")
    public SuccessResponse<Void> logout(
            @CurrentUser User user,
            @Valid @RequestBody LogoutRequest request
    ) {
        authTokenService.logout(user.getId(), request.deviceId());
        return SuccessResponse.ok();
    }


    @PostMapping("/apple")
    @Operation(
            summary = "애플 네이티브 앱 로그인",
            description = "Flutter iOS/Android에서 Apple 로그인 후 받은 identityToken을 검증하고 POZIT JWT를 발급합니다. platform 값에 따라 iOS는 bundleId, Android는 serviceId로 audience를 검증하며, nonce claim도 함께 검증합니다. deviceId는 Apple에서 받는 값이 아니라 앱 클라이언트가 생성한 UUID이며, secure storage 등에 저장하고 같은 기기의 로그인/재발급/로그아웃 요청에서 동일하게 전달해야 합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Apple 로그인 성공 후 클라이언트가 받은 토큰 및 플랫폼 정보",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = AppleIdentityTokenRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "iOS Flutter 요청",
                                    value = """
                                            {
                                              "identityToken": "eyJraWQiOiJ...",
                                              "authorizationCode": "c1234567890...",
                                              "nonce": "d8f3b2a1c9",
                                              "platform": "IOS",
                                              "deviceId": "550e8400-e29b-41d4-a716-446655440000",
                                              "email": "user@example.com",
                                              "givenName": "Minseo",
                                              "familyName": "Kim"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Android Flutter 요청",
                                    value = """
                                            {
                                              "identityToken": "eyJraWQiOiJ...",
                                              "authorizationCode": "c1234567890...",
                                              "nonce": "d8f3b2a1c9",
                                              "platform": "ANDROID",
                                              "deviceId": "550e8400-e29b-41d4-a716-446655440000",
                                              "email": "user@example.com",
                                              "givenName": "Minseo",
                                              "familyName": "Kim"
                                            }
                                            """
                            )
                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공 및 POZIT JWT 발급",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "요청에 성공했습니다.",
                                      "result": {
                                        "accessToken": "pozit_access_token",
                                        "refreshToken": "pozit_refresh_token",
                                        "tokenType": "Bearer",
                                        "expiresIn": 1800,
                                        "refreshTokenExpiresIn": 1209600,
                                        "userId": 1,
                                        "nickname": "Minseo Kim",
                                        "isNewUser": true
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
                                      "errors": {
                                        "nonce": "공백일 수 없습니다."
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Apple identityToken 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "APPLELOGIN401_1",
                                      "message": "유효하지 않은 Apple identity token입니다."
                                    }
                                    """)
                    )
            )
    })
    public SuccessResponse<LoginTokenResponse> appleNativeLogin(
            @Valid @RequestBody AppleIdentityTokenRequest request
            ){
        LoginTokenResponse response=authAppleService.loginWithApple(request);
        return SuccessResponse.ok(response);

    }



    @PostMapping("/apple/callback")
    public void appleCallback(
            @RequestParam String code,
            @RequestParam("id_token") String identityToken,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String user,
            HttpServletResponse response
    ) throws IOException {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("intent://callback")
                .queryParam("code", code)
                .queryParam("id_token", identityToken);

        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }

        if (user != null && !user.isBlank()) {
            builder.queryParam("user", user);
        }

        String redirectUrl = builder
                .fragment("Intent;package=com.pozit.pozit;scheme=signinwithapple;end")
                .build()
                .encode()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

}
