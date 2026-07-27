package com.pozit.pozitserver.global.auth.controller;

import com.pozit.pozitserver.global.auth.dto.request.KakaoAccessTokenRequest;
import com.pozit.pozitserver.global.auth.dto.response.LoginTokenResponse;
import com.pozit.pozitserver.global.auth.ios.AppleIdentityTokenRequest;
import com.pozit.pozitserver.global.auth.kakao.KakaoProperties;
import com.pozit.pozitserver.global.auth.service.AuthAppleService;
import com.pozit.pozitserver.global.auth.service.AuthKakaoService;
import com.pozit.pozitserver.global.response.ErrorResponse;
import com.pozit.pozitserver.global.response.SuccessResponse;
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
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name="소셜 로그인 API",description = "소셜 로그인 관련 API 입니다.")
public class AuthController {

    private final KakaoProperties kakaoProperties;
    private final AuthKakaoService authService;
    private final AuthAppleService authAppleService;

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


    @PostMapping("/apple")
    @Operation(
            summary = "애플 네이티브 앱 로그인",
            description = "Flutter iOS/Android에서 Apple 로그인 후 받은 identityToken을 검증하고 POZIT JWT를 발급합니다. platform 값에 따라 iOS는 bundleId, Android는 serviceId로 audience를 검증하며, nonce claim도 함께 검증합니다."
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
                                        "tokenType": "Bearer",
                                        "expiresIn": 1800,
                                        "userId": 1,
                                        "nickname": "Minseo Kim"
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
    public Map<String, Object> appleCallback(
            @RequestParam String code,
            @RequestParam("id_token") String identityToken,
            @RequestParam String state,
            @RequestParam(required = false) String user
    ) {
        return Map.of(
                "authorizationCode", code,
                "identityToken", identityToken,
                "state", state,
                "user", user == null ? "" : user
        );
    }

}
