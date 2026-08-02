package com.pozit.pozitserver.global.auth.apple;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class AppleClientSecretProvider {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";

    private final AppleProperties appleProperties;

    public String createClientSecret(String clientId) {
        validateProperties();

        Instant now = Instant.now();
        Instant expiresAt = now.plus(180, ChronoUnit.DAYS);

        return Jwts.builder()
                .header()
                .keyId(appleProperties.keyId())
                .and()
                .issuer(appleProperties.teamId())
                .subject(clientId)
                .audience()
                .add(APPLE_AUDIENCE)
                .and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(parsePrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    private void validateProperties() {
        if (isBlank(appleProperties.teamId())
                || isBlank(appleProperties.keyId())
                || isBlank(appleProperties.privateKey())) {
            throw new BusinessException(ErrorCode.APPLE_CLIENT_SECRET_CONFIG_MISSING);
        }
    }

    private PrivateKey parsePrivateKey() {
        try {
            String privateKey = appleProperties.privateKey()
                    .replace("\\n", "\n")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("EC").generatePrivate(keySpec);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.APPLE_CLIENT_SECRET_CONFIG_MISSING);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
