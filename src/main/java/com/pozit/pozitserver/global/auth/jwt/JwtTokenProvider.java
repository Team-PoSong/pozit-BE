package com.pozit.pozitserver.global.auth.jwt;
import com.pozit.pozitserver.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(
            JwtEncoder jwtEncoder,
            JwtDecoder jwtDecoder,
            JwtProperties jwtProperties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.jwtProperties = jwtProperties;
    }

    public String createAccessToken(User user) {

        return createToken(
                user,
                TokenType.ACCESS,
                jwtProperties.accessTokenExpiration()
        );
    }

    public String createRefreshToken(User user) {
        return createToken(
                user,
                TokenType.REFRESH,
                jwtProperties.refreshTokenExpiration()
        );
    }

    private String createToken(
            User user,
            TokenType tokenType,
            long expirationSeconds
    ) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, tokenType.name())
                .build();

        JwsHeader header = JwsHeader
                .with(() -> JwsAlgorithms.HS256)
                .type("JWT")
                .build();

        JwtEncoderParameters parameters =
                JwtEncoderParameters.from(header, claims);

        return jwtEncoder.encode(parameters).getTokenValue();
    }

    public Jwt decodeToken(String token) {
        return jwtDecoder.decode(token);
    }

    public Long getMemberId(String token) {
        Jwt jwt = decodeToken(token);

        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "JWT의 회원 ID 형식이 올바르지 않습니다.",
                    exception
            );
        }
    }

    public String getRole(String token) {
        Jwt jwt = decodeToken(token);
        return jwt.getClaimAsString(CLAIM_ROLE);
    }

    public TokenType getTokenType(String token) {
        Jwt jwt = decodeToken(token);
        String tokenType = jwt.getClaimAsString(CLAIM_TYPE);

        try {
            return TokenType.valueOf(tokenType);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException(
                    "JWT의 토큰 종류가 올바르지 않습니다.",
                    exception
            );
        }
    }

    public void validateRefreshToken(String token) {
        Jwt jwt = decodeToken(token);
        String tokenType = jwt.getClaimAsString(CLAIM_TYPE);

        if (!TokenType.REFRESH.name().equals(tokenType)) {
            throw new IllegalArgumentException(
                    "Refresh Token이 아닙니다."
            );
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpiration();
    }

    public long getRefreshTokenExpirationSeconds() {
        return jwtProperties.refreshTokenExpiration();
    }
}
