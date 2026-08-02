package com.pozit.pozitserver.global.auth.service;

import com.pozit.pozitserver.global.auth.dto.response.LoginTokenResponse;
import com.pozit.pozitserver.global.auth.jwt.JwtTokenProvider;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    public LoginTokenResponse issueLoginTokens(
            User user,
            String deviceId,
            boolean isNewUser
    ) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        saveRefreshToken(user.getId(), deviceId, refreshToken);

        return LoginTokenResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                jwtTokenProvider.getRefreshTokenExpirationSeconds(),
                user,
                isNewUser
        );
    }

    public LoginTokenResponse reissue(
            String refreshToken,
            String deviceId
    ) {
        Long userId = validateStoredRefreshToken(refreshToken, deviceId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON401));

        return issueLoginTokens(user, deviceId, false);
    }

    public void logout(
            Long userId,
            String deviceId
    ) {
        stringRedisTemplate.delete(refreshTokenKey(userId, deviceId));
    }

    public void logoutAllDevices(Long userId) {
        Set<String> keys = scanRefreshTokenKeys(userId);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        stringRedisTemplate.delete(keys);
    }

    private Set<String> scanRefreshTokenKeys(Long userId) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(refreshTokenKeyPattern(userId))
                .count(1000)
                .build();

        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.COMMON500);
        }

        return keys;
    }

    private Long validateStoredRefreshToken(
            String refreshToken,
            String deviceId
    ) {
        Long userId = jwtTokenProvider.getRefreshTokenMemberId(refreshToken);

        String savedTokenHash =
                stringRedisTemplate.opsForValue().get(refreshTokenKey(userId, deviceId));

        if (!Objects.equals(savedTokenHash, hash(refreshToken))) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return userId;
    }

    private void saveRefreshToken(
            Long userId,
            String deviceId,
            String refreshToken
    ) {
        stringRedisTemplate.opsForValue().set(
                refreshTokenKey(userId, deviceId),
                hash(refreshToken),
                jwtTokenProvider.getRefreshTokenExpirationSeconds(),
                TimeUnit.SECONDS
        );
    }

    private String refreshTokenKey(
            Long userId,
            String deviceId
    ) {
        return REFRESH_TOKEN_KEY_PREFIX + userId + ":" + deviceId;
    }

    private String refreshTokenKeyPattern(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId + ":*";
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
