package com.pozit.pozitserver.global.auth.service;

import com.pozit.pozitserver.global.auth.dto.response.LoginTokenResponse;
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

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthKakaoService {

    private final KakaoClient kakaoClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginTokenResponse loginWithKakao(String authorizationCode) {
        KakaoTokenResponse kakaoToken =
                kakaoClient.requestAccessToken(authorizationCode);

        return loginWithKakaoAccessToken(kakaoToken.accessToken());
    }

    public LoginTokenResponse loginWithKakaoAccessToken(String kakaoAccessToken) {
        KakaoUserResponse kakaoUser =
                kakaoClient.requestUserInfo(kakaoAccessToken);

        Optional<User> optionalUser = userRepository.findByProviderAndSocialId(
                SocialProvider.KAKAO,
                kakaoUser.id().toString()
        );

        boolean isNewUser = optionalUser.isEmpty();
        User user = optionalUser
                .map(existingUser -> {
                    existingUser.updateProfile(kakaoUser.getNickname());
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .provider(SocialProvider.KAKAO)
                                .socialId(kakaoUser.id().toString())
                                .nickname(kakaoUser.getNickname())
                                .role(Role.USER)
                                .build()
                ));

        String accessToken=jwtTokenProvider.createAccessToken(user);
        return LoginTokenResponse.of(
                accessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                user,
                isNewUser
        );
    }
}
