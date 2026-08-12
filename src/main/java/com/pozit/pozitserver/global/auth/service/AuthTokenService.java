package com.pozit.pozitserver.global.auth.service;

import com.pozit.pozitserver.global.auth.dto.response.LoginTokenResponse;
import com.pozit.pozitserver.global.auth.jwt.JwtTokenProvider;
import com.pozit.pozitserver.global.auth.jwt.TokenType;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final String ACCESS_TOKEN_BLACKLIST_KEY_PREFIX = "auth:blacklist:access:";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_VALUE = "logout";

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    public LoginTokenResponse issueLoginTokens(
            User user,
            boolean isNewUser
    ) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        saveRefreshToken(user.getId(), refreshToken);

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
            String refreshToken
    ) {
        Long userId = validateStoredRefreshToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON401));

        return issueLoginTokens(user, false);
    }

    public void logout(
            Long userId,
            String authorizationHeader
    ) {
        blacklistAccessToken(extractBearerToken(authorizationHeader));
        stringRedisTemplate.delete(refreshTokenKey(userId));
    }

    public void logoutAllDevices(Long userId) {
        Set<String> keys = scanRefreshTokenKeys(userId);
        keys.add(refreshTokenKey(userId));
        stringRedisTemplate.delete(keys);
    }

    public void validateAccessTokenNotBlacklisted(Jwt jwt) {
        String tokenId = jwt.getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON401);
        }

        Boolean hasKey = stringRedisTemplate.hasKey(accessTokenBlacklistKey(tokenId));
        if (Boolean.TRUE.equals(hasKey)) {
            throw new BusinessException(ErrorCode.COMMON401);
        }
    }

    private void blacklistAccessToken(String accessToken) {
        try {
            Jwt jwt = jwtTokenProvider.decodeToken(accessToken);
            jwtTokenProvider.validateTokenType(jwt, TokenType.ACCESS);

            String tokenId = jwt.getId();
            Instant expiresAt = jwt.getExpiresAt();
            if (tokenId == null || tokenId.isBlank() || expiresAt == null) {
                throw new BusinessException(ErrorCode.COMMON401);
            }

            long ttlSeconds = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
            if (ttlSeconds <= 0) {
                return;
            }

            stringRedisTemplate.opsForValue().set(
                    accessTokenBlacklistKey(tokenId),
                    BLACKLIST_VALUE,
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        } catch (JwtException | BusinessException exception) {
            throw new BusinessException(ErrorCode.COMMON401);
        }
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
            String refreshToken
    ) {
        Long userId = jwtTokenProvider.getRefreshTokenMemberId(refreshToken);

        String savedTokenHash =
                stringRedisTemplate.opsForValue().get(refreshTokenKey(userId));

        String requestTokenHash = hash(refreshToken);
        if (Objects.equals(savedTokenHash, requestTokenHash)) {
            return userId;
        }

        boolean matchesLegacyKey = scanRefreshTokenKeys(userId).stream()
                .map(key -> stringRedisTemplate.opsForValue().get(key))
                .anyMatch(tokenHash -> Objects.equals(tokenHash, requestTokenHash));

        if (!matchesLegacyKey) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return userId;
    }

    private void saveRefreshToken(
            Long userId,
            String refreshToken
    ) {
        stringRedisTemplate.opsForValue().set(
                refreshTokenKey(userId),
                hash(refreshToken),
                jwtTokenProvider.getRefreshTokenExpirationSeconds(),
                TimeUnit.SECONDS
        );
    }

    private String refreshTokenKey(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId;
    }

    private String refreshTokenKeyPattern(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId + ":*";
    }

    private String accessTokenBlacklistKey(String tokenId) {
        return ACCESS_TOKEN_BLACKLIST_KEY_PREFIX + tokenId;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new BusinessException(ErrorCode.COMMON401);
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON401);
        }

        return token;
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
