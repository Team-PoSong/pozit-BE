package com.pozit.pozitserver.global.auth.service;

import com.pozit.pozitserver.global.auth.dto.response.LoginTokenResponse;
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
    private final AuthTokenService authTokenService;
    private final AuthNicknameService authNicknameService;

    public LoginTokenResponse loginWithKakao(String authorizationCode) {
        KakaoTokenResponse kakaoToken =
                kakaoClient.requestAccessToken(authorizationCode);

        return loginWithKakaoAccessToken(kakaoToken.accessToken());
    }

    public LoginTokenResponse loginWithKakaoAccessToken(String kakaoAccessToken) {
        KakaoUserResponse kakaoUser =
                kakaoClient.requestUserInfo(kakaoAccessToken);

        String socialId = kakaoUser.id().toString();
        String nickname = kakaoUser.getNickname();

        Optional<User> optionalUser = userRepository.findByProviderAndSocialId(
                SocialProvider.KAKAO,
                socialId
        );

        boolean isNewUser = false;
        User user;

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
        } else {
            isNewUser = true;
            user = createUser(socialId, nickname);
        }

        return authTokenService.issueLoginTokens(user, isNewUser);
    }

    private User createUser(String socialId, String nickname) {
        return userRepository.save(
                User.builder()
                        .provider(SocialProvider.KAKAO)
                        .socialId(socialId)
                        .nickname(authNicknameService.resolveAvailableNickname(
                                nickname,
                                null,
                                SocialProvider.KAKAO,
                                socialId
                        ))
                        .role(Role.USER)
                        .build()
        );
    }
}
