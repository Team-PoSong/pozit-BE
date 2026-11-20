package com.pozit.pozitserver.global.auth.service;

import com.pozit.pozitserver.global.auth.dto.response.LoginTokenResponse;
import com.pozit.pozitserver.global.auth.google.GoogleIdTokenVerifier;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.user.domain.SocialProvider;
import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class AuthGoogleService {

    private static final int MAX_REGISTRATION_ATTEMPTS = 3;

    private final GoogleIdTokenVerifier verifier;
    private final UserRepository userRepository;
    private final AuthNicknameService authNicknameService;
    private final AuthTokenService authTokenService;
    private final TransactionTemplate transactionTemplate;

    public LoginTokenResponse loginWithGoogle(String idToken) {
        String socialId = verifier.verifyAndGetSubject(idToken);
        LoginAccount account = transactionTemplate.execute(status -> findOrCreateAccount(socialId));
        return authTokenService.issueLoginTokens(account.user(), account.newUser());
    }

    private LoginAccount findOrCreateAccount(String socialId) {
        User existing = userRepository.findByProviderAndSocialId(SocialProvider.GOOGLE, socialId).orElse(null);
        if (existing != null) {
            return existingAccount(existing);
        }

        for (int attempt = 0; attempt < MAX_REGISTRATION_ATTEMPTS; attempt++) {
            String nickname = authNicknameService.resolveAvailableNickname(null, null, SocialProvider.GOOGLE, socialId);
            boolean inserted = userRepository.insertGoogleUserIfAbsent(socialId, nickname) == 1;
            User account = userRepository.findByProviderAndSocialId(SocialProvider.GOOGLE, socialId).orElse(null);
            if (account != null) {
                return inserted ? new LoginAccount(account, true) : existingAccount(account);
            }
        }
        throw new BusinessException(ErrorCode.COMMON409);
    }

    private LoginAccount existingAccount(User user) {
        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMON401);
        }
        return new LoginAccount(user, false);
    }

    private record LoginAccount(User user, boolean newUser) {
    }
}
