package com.pozit.pozitserver.global.auth.kakao;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.kakao")
public record KakaoProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String frontendRedirectUri
) {
}
