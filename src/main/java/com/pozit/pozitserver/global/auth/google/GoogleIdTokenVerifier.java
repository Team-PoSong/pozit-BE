package com.pozit.pozitserver.global.auth.google;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Component
public class GoogleIdTokenVerifier {

    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String GOOGLE_LEGACY_ISSUER = "accounts.google.com";
    private static final Duration KEY_REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final String serverClientId;
    private final NimbusJwtDecoder decoder;

    @Autowired
    public GoogleIdTokenVerifier(@Value("${oauth.google.server-client-id:}") String serverClientId) {
        this(serverClientId, keyRestTemplate());
    }

    GoogleIdTokenVerifier(String serverClientId, RestOperations keyClient) {
        this.serverClientId = serverClientId.strip();
        this.decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .restOperations(keyClient)
                .build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setAllowEmptyExpiryClaim(false);
        decoder.setJwtValidator(timestamps);
    }

    public String verifyAndGetSubject(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new BusinessException(ErrorCode.GOOGLE_INVALID_ID_TOKEN);
        }
        if (serverClientId.isEmpty()) {
            throw new BusinessException(ErrorCode.GOOGLE_CLIENT_ID_MISSING);
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (BadJwtException exception) {
            throw new BusinessException(ErrorCode.GOOGLE_INVALID_ID_TOKEN);
        } catch (JwtException exception) {
            throw new BusinessException(ErrorCode.GOOGLE_TOKEN_VERIFICATION_FAILED, exception);
        }

        String issuer = jwt.getClaimAsString(JwtClaimNames.ISS);
        boolean validIssuer = GOOGLE_ISSUER.equals(issuer) || GOOGLE_LEGACY_ISSUER.equals(issuer);
        String subject = jwt.getSubject();
        if (!validIssuer || !List.of(serverClientId).equals(jwt.getAudience()) || !StringUtils.hasText(subject)) {
            throw new BusinessException(ErrorCode.GOOGLE_INVALID_ID_TOKEN);
        }
        return subject;
    }

    private static RestTemplate keyRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(KEY_REQUEST_TIMEOUT);
        factory.setReadTimeout(KEY_REQUEST_TIMEOUT);
        return new RestTemplate(factory);
    }
}
