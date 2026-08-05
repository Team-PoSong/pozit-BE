package com.pozit.pozitserver.global.auth.service;

import com.pozit.pozitserver.global.auth.apple.AppleClient;
import com.pozit.pozitserver.global.auth.apple.jwt.AppleTokenClaims;
import com.pozit.pozitserver.global.auth.dto.response.LoginTokenResponse;
import com.pozit.pozitserver.global.auth.ios.AppleIdentityTokenRequest;
import com.pozit.pozitserver.user.domain.Role;
import com.pozit.pozitserver.user.domain.SocialProvider;
import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthAppleService {

    private final AppleClient appleClient;
    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;
    private final AuthNicknameService authNicknameService;

    public LoginTokenResponse loginWithApple(AppleIdentityTokenRequest request) {
        AppleTokenClaims claims = appleClient.loginWithAppleIdentityToken(request);
        String nickname = resolveNickname(request, claims);

        String socialId = claims.socialId();

        Optional<User> optionalUser = userRepository.findByProviderAndSocialId(
                SocialProvider.APPLE,
                socialId
        );

        boolean isNewUser = false;
        User user;

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            user.updateProfile(authNicknameService.resolveAvailableNickname(
                    nickname,
                    user.getId(),
                    SocialProvider.APPLE,
                    socialId
            ));
        } else {
            isNewUser = true;
            user = createUser(socialId, nickname);
        }

        return authTokenService.issueLoginTokens(user, request.deviceId(), isNewUser);
    }

    private User createUser(String socialId, String nickname) {
        return userRepository.save(
                User.builder()
                        .provider(SocialProvider.APPLE)
                        .socialId(socialId)
                        .nickname(authNicknameService.resolveAvailableNickname(
                                nickname,
                                null,
                                SocialProvider.APPLE,
                                socialId
                        ))
                        .role(Role.USER)
                        .build()
        );
    }

    private String resolveNickname(
            AppleIdentityTokenRequest request,
            AppleTokenClaims claims
    ) {
        String requestName = joinName(request.givenName(), request.familyName());
        if (!requestName.isBlank()) {
            return requestName;
        }

        if (claims.email() != null && !claims.email().isBlank()) {
            int atIndex = claims.email().indexOf("@");
            return atIndex > 0 ? claims.email().substring(0, atIndex) : claims.email();
        }

        String socialId = claims.socialId();
        return "apple_" + socialId.substring(Math.max(0, socialId.length() - 8));
    }

    private String joinName(String givenName, String familyName) {
        String firstName = givenName == null ? "" : givenName.trim();
        String lastName = familyName == null ? "" : familyName.trim();
        return (firstName + " " + lastName).trim();
    }
}
