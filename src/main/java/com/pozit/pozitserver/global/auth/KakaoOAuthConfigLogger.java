package com.pozit.pozitserver.global.auth;

import com.pozit.pozitserver.global.auth.kakao.KakaoProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuthConfigLogger {

    private final KakaoProperties kakaoOAuthProperties;
    private final Environment environment;

    @PostConstruct
    public void logConfig() {
        log.info(
                "Active profiles: {}",
                Arrays.toString(environment.getActiveProfiles())
        );

        log.info(
                "Kakao redirect URI: {}",
                kakaoOAuthProperties.redirectUri()
        );
    }
}