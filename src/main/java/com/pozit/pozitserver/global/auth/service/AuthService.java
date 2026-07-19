package com.pozit.pozitserver.global.auth.service;

import com.pozit.pozitserver.global.auth.dto.LoginTokenResponse;
import com.pozit.pozitserver.global.auth.jwt.JwtTokenProvider;
import com.pozit.pozitserver.global.auth.kakao.KakaoClient;
import com.pozit.pozitserver.user.domain.Role;
import com.pozit.pozitserver.user.domain.SocialProvider;
import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.dto.response.KakaoTokenResponse;
import com.pozit.pozitserver.user.dto.response.KakaoUserResponse;
import com.pozit.pozitserver.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final KakaoClient kakaoClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginTokenResponse loginWithKakao(String authorizationCode) {
        KakaoTokenResponse kakaoToken =
                kakaoClient.requestAccessToken(authorizationCode);
        log.info("kakaoToken:{}",kakaoToken);

        KakaoUserResponse kakaoUser =
                kakaoClient.requestUserInfo(kakaoToken.accessToken());
        log.info("kakaoUser:{}",kakaoUser);

        User user = userRepository
                .findByProviderAndSocialId(
                        SocialProvider.KAKAO,
                        kakaoUser.id().toString()
                )
                .map(existingUser -> {
                    existingUser.updateProfile(
//                            kakaoUser.getEmail(),
                            kakaoUser.getNickname()
                    );
                    log.info("existingUser:{}",existingUser);
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(

                        User.builder()
                                .provider(SocialProvider.KAKAO)
                                .socialId(kakaoUser.id().toString())
//                                .email(kakaoUser.getEmail())
                                .nickname(kakaoUser.getNickname())
                                .role(Role.USER)
                                .build()
                ));
        log.info(
                "userId={}, nickname={}, role={}, provider={}, socialId={}",
                user.getId(),
                user.getNickname(),
                user.getRole(),
                user.getProvider(),
                user.getSocialId()
        );
        log.info("JWT 생성 직전");
        String accessToken=jwtTokenProvider.createAccessToken(user);
        log.info("JWT 생성 완료");
        return LoginTokenResponse.of(
                accessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                user
        );
    }
}