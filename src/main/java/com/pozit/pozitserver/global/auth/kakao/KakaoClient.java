package com.pozit.pozitserver.global.auth.kakao;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.user.dto.response.KakaoTokenResponse;
import com.pozit.pozitserver.user.dto.response.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
//카카오 서버와 직접 통신하는 외부 API 클라이언트
public class KakaoClient {

    private final WebClient.Builder webClientBuilder;
    private final KakaoProperties kakaoProperties;

    public KakaoTokenResponse requestAccessToken(String authorizationCode) {
        return webClientBuilder.build()
                .post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type", "authorization_code")
                        .with("client_id", kakaoProperties.clientId())
                        .with("redirect_uri", kakaoProperties.redirectUri())
                        .with("code", authorizationCode)
                        .with("client_secret", kakaoProperties.clientSecret()))
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .block();
    }

    public KakaoUserResponse requestUserInfo(String kakaoAccessToken) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .headers(headers ->
                            headers.setBearerAuth(normalizeAccessToken(kakaoAccessToken)))
                    .retrieve()
                    .bodyToMono(KakaoUserResponse.class)
                    .block();
        } catch (WebClientResponseException.Unauthorized e) {
            throw new BusinessException(ErrorCode.KAKAO_INVALID_ACCESS_TOKEN);
        }
    }

    private String normalizeAccessToken(String accessToken) {
        String trimmedAccessToken = accessToken.trim();

        if (trimmedAccessToken.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmedAccessToken.substring(7).trim();
        }

        return trimmedAccessToken;
    }
}
