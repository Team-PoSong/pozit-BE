package com.pozit.pozitserver.global.auth.service;

import com.pozit.pozitserver.global.auth.apple.AppleClient;
import com.pozit.pozitserver.global.auth.apple.jwt.AppleTokenClaims;
import com.pozit.pozitserver.global.auth.dto.response.LoginTokenResponse;
import com.pozit.pozitserver.global.auth.ios.AppleIdentityTokenRequest;
import com.pozit.pozitserver.global.auth.jwt.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;

    public LoginTokenResponse loginWithApple(AppleIdentityTokenRequest request) {
        AppleTokenClaims claims = appleClient.loginWithAppleIdentityToken(request);
        String nickname = resolveNickname(request, claims);

        Optional<User> optionalUser = userRepository.findByProviderAndSocialId(
                SocialProvider.APPLE,
                claims.socialId()
        );

        boolean isNewUser = optionalUser.isEmpty();
        User user = optionalUser
                .map(existingUser -> {
                    existingUser.updateProfile(nickname);
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .provider(SocialProvider.APPLE)
                                .socialId(claims.socialId())
                                .nickname(nickname)
                                .role(Role.USER)
                                .build()
                ));

        String accessToken = jwtTokenProvider.createAccessToken(user);
        return LoginTokenResponse.of(
                accessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                user,
                isNewUser
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
