package com.pozit.pozitserver.global.auth.service;

import com.pozit.pozitserver.user.domain.SocialProvider;
import com.pozit.pozitserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthNicknameService {

    private static final int MAX_ATTEMPTS = 100;

    private final UserRepository userRepository;

    public String resolveAvailableNickname(
            String desiredNickname,
            Long currentUserId,
            SocialProvider provider,
            String socialId
    ) {
        String baseNickname = normalizeNickname(desiredNickname, provider, socialId);

        if (isAvailable(baseNickname, currentUserId)) {
            return baseNickname;
        }

        String suffixSeed = socialId.substring(Math.max(0, socialId.length() - 4));
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            String candidate = baseNickname + "_" + suffixSeed + i;
            if (isAvailable(candidate, currentUserId)) {
                return candidate;
            }
        }

        return provider.name().toLowerCase() + "_" + System.nanoTime();
    }

    private String normalizeNickname(String desiredNickname, SocialProvider provider, String socialId) {
        if (desiredNickname != null && !desiredNickname.trim().isBlank()) {
            return desiredNickname.trim();
        }

        String suffix = socialId.substring(Math.max(0, socialId.length() - 8));
        return provider.name().toLowerCase() + "_" + suffix;
    }

    private boolean isAvailable(String nickname, Long currentUserId) {
        if (currentUserId == null) {
            return !userRepository.existsByNickname(nickname);
        }

        return !userRepository.existsByNicknameAndIdNot(nickname, currentUserId);
    }
}
