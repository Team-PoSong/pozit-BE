package com.pozit.pozitserver.global.auth.google;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleIdTokenVerifierTest {

    private static final String GOOGLE_KEYS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String ISSUER = "https://accounts.google.com";
    private static final String SERVER_CLIENT_ID = "pozit-test.apps.googleusercontent.com";
    private static final String SUBJECT = "google-account-subject";

    private static RSAKey signingKey;
    private static RSAKey rotatedKey;
    private RestTemplate keyClient;
    private MockRestServiceServer keyServer;
    private GoogleIdTokenVerifier verifier;

    @BeforeAll
    static void createSigningKeys() throws JOSEException {
        signingKey = new RSAKeyGenerator(2048).keyID("google-key-1").generate();
        rotatedKey = new RSAKeyGenerator(2048).keyID("google-key-2").generate();
    }

    @BeforeEach
    void createVerifier() {
        keyClient = new RestTemplate();
        keyServer = MockRestServiceServer.bindTo(keyClient).build();
        verifier = new GoogleIdTokenVerifier(SERVER_CLIENT_ID, keyClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {ISSUER, "accounts.google.com"})
    void returnsSubjectForEitherGoogleIssuer(String issuer) throws JOSEException {
        publishKey(signingKey);
        String token = sign(validClaims().issuer(issuer), signingKey, signingKey.getKeyID());

        assertThat(verifier.verifyAndGetSubject(token)).isEqualTo(SUBJECT);
        keyServer.verify();
    }

    @Test
    void reusesCachedKeyAndRefreshesWhenGoogleRotatesIt() throws JOSEException {
        publishKey(signingKey);
        publishKey(rotatedKey);
        String originalToken = sign(validClaims(), signingKey, signingKey.getKeyID());
        String rotatedToken = sign(validClaims(), rotatedKey, rotatedKey.getKeyID());

        assertThat(verifier.verifyAndGetSubject(originalToken)).isEqualTo(SUBJECT);
        assertThat(verifier.verifyAndGetSubject(originalToken)).isEqualTo(SUBJECT);
        assertThat(verifier.verifyAndGetSubject(rotatedToken)).isEqualTo(SUBJECT);
        keyServer.verify();
    }

    @Test
    void rejectsInvalidSignatureForKnownKeyId() throws JOSEException {
        publishKey(signingKey);

        assertInvalid(sign(validClaims(), rotatedKey, signingKey.getKeyID()));
    }

    @Test
    void rejectsUnexpectedIssuer() throws JOSEException {
        publishKey(signingKey);

        assertInvalid(sign(validClaims().issuer("https://example.org"), signingKey, signingKey.getKeyID()));
    }

    @Test
    void rejectsUnexpectedOrAdditionalAudiences() throws JOSEException {
        publishKey(signingKey);

        assertInvalid(sign(validClaims().audience("another-client"), signingKey, signingKey.getKeyID()));
        assertInvalid(sign(validClaims().audience(List.of(SERVER_CLIENT_ID, "another-client")),
                signingKey, signingKey.getKeyID()));
    }

    @Test
    void rejectsExpiredTokensAndTokensWithoutExpiry() throws JOSEException {
        publishKey(signingKey);

        assertInvalid(sign(validClaims().expirationTime(Date.from(Instant.now().minusSeconds(5))),
                signingKey, signingKey.getKeyID()));
        assertInvalid(sign(validClaims().expirationTime(null), signingKey, signingKey.getKeyID()));
    }

    @Test
    void rejectsTokensWithoutSubject() throws JOSEException {
        publishKey(signingKey);

        assertInvalid(sign(validClaims().subject(null), signingKey, signingKey.getKeyID()));
    }

    @Test
    void rejectsMalformedInputWithoutFetchingKeys() {
        assertInvalid("not-a-jwt");
        keyServer.verify();
    }

    @Test
    void readsServerClientIdFromApplicationYaml() throws IOException {
        var yaml = new ByteArrayResource("""
                oauth:
                  google:
                    server-client-id: %s
                """.formatted(SERVER_CLIENT_ID).getBytes(StandardCharsets.UTF_8));
        var properties = new YamlPropertySourceLoader().load("test-application", yaml).get(0);

        new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(properties))
                .withUserConfiguration(GoogleIdTokenVerifier.class)
                .run(context -> assertThatThrownBy(() -> context.getBean(GoogleIdTokenVerifier.class)
                        .verifyAndGetSubject("not-a-jwt"))
                        .isInstanceOfSatisfying(BusinessException.class,
                                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_INVALID_ID_TOKEN)));
    }

    @Test
    void reportsMissingConfigurationWithoutFetchingKeys() {
        GoogleIdTokenVerifier unconfigured = new GoogleIdTokenVerifier(" ", keyClient);

        assertThatThrownBy(() -> unconfigured.verifyAndGetSubject("id-token"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_CLIENT_ID_MISSING));
        keyServer.verify();
    }

    @Test
    void reportsKeyEndpointFailureSeparatelyFromAnInvalidToken() throws JOSEException {
        keyServer.expect(requestTo(GOOGLE_KEYS_URI)).andRespond(withServerError());
        String token = sign(validClaims(), signingKey, signingKey.getKeyID());

        assertThatThrownBy(() -> verifier.verifyAndGetSubject(token))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_TOKEN_VERIFICATION_FAILED));
        keyServer.verify();
    }

    private void publishKey(RSAKey key) {
        keyServer.expect(requestTo(GOOGLE_KEYS_URI))
                .andRespond(withSuccess(new JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON));
    }

    private void assertInvalid(String token) {
        assertThatThrownBy(() -> verifier.verifyAndGetSubject(token))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_INVALID_ID_TOKEN));
    }

    private static JWTClaimsSet.Builder validClaims() {
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .audience(SERVER_CLIENT_ID)
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(300)));
    }

    private static String sign(JWTClaimsSet.Builder claims, RSAKey key, String keyId) throws JOSEException {
        SignedJWT token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(), claims.build());
        token.sign(new RSASSASigner(key));
        return token.serialize();
    }
}
