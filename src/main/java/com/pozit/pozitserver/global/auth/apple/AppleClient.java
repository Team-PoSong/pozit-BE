package com.pozit.pozitserver.global.auth.apple;

import com.pozit.pozitserver.global.auth.apple.jwt.AppleJwtHeader;
import com.pozit.pozitserver.global.auth.apple.jwt.AppleTokenClaims;
import com.pozit.pozitserver.global.auth.dto.response.apple.ApplePublicKey;
import com.pozit.pozitserver.global.auth.dto.response.apple.ApplePublicKeyResponse;
import com.pozit.pozitserver.global.auth.dto.response.apple.AppleTokenResponse;
import com.pozit.pozitserver.global.auth.ios.AppleIdentityTokenRequest;
import com.pozit.pozitserver.global.auth.ios.ApplePlatform;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Collection;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AppleClient {

    private static final String APPLE_TOKEN_URI = "https://appleid.apple.com/auth/token";
    private static final String APPLE_REVOKE_URI = "https://appleid.apple.com/auth/revoke";
    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    private static final String TOKEN_TYPE_HINT_ACCESS_TOKEN = "access_token";
    private static final String TOKEN_TYPE_HINT_REFRESH_TOKEN = "refresh_token";

    private final AppleJwtProvider appleJwtProvider;
    private final ApplePublicKeyGenerator applePublicKeyGenerator;
    private final AppleClientSecretProvider appleClientSecretProvider;
    private final AppleProperties appleProperties;
    private final WebClient.Builder webClientBuilder;

    public AppleTokenClaims loginWithAppleIdentityToken(AppleIdentityTokenRequest request){
        String identityToken=request.identityToken();

        // identity token header JWT 디코딩
        AppleJwtHeader header=appleJwtProvider.parseHeader(identityToken);

        //apple server의 전체 공개 키 목록 조회
        ApplePublicKeyResponse publicKeys=getPublicKey();

        //헤더의 key, alg와 일치하는 공개키 선택
        ApplePublicKey matchedKey=publicKeys.getMatchedKey(
                header.kid(),
                header.alg()
        );

        //Apple JWK를 Java RSA 공개키로 변환
        PublicKey publicKey=applePublicKeyGenerator.generatePublicKey(matchedKey);

        //서명 검증 및 기본 claims 검증
        Claims claims=appleJwtProvider.verifyAndParseToken(
                identityToken,
                publicKey
        );

        //iss 필드 검사
        if(!appleProperties.issuer().equals(claims.getIssuer())){
            throw new BusinessException(ErrorCode.INVALID_APPLE_TOKEN_ISSUE);
        }

        //audience 검증
        String expectedAudience=switch(request.platform()){
            case IOS -> appleProperties.bundleId();
            case ANDROID -> appleProperties.serviceId();
        };
        validateAudience(claims, expectedAudience);
        validateNonce(claims, request.nonce());

        //애플 회원 고유 식별값
        String appleSocialId=claims.getSubject();
        if(appleSocialId==null||appleSocialId.isBlank()){
            throw new BusinessException(ErrorCode.NOT_FOUND_APPLE_IDENTITY_TOKEN_SUBJECT);
        }

        //검증된 claims에서 사용자 정보 추출
        return new AppleTokenClaims(
                appleSocialId,
                claims.get("email",String.class),
                parseBooleanClaim(claims.get("email_verified")),
                parseBooleanClaim(claims.get("is_private_email"))
        );

    }

    public void revokeAuthorizationCode(
            String authorizationCode,
            ApplePlatform platform
    ) {
        validateAuthorizationCodeRequest(authorizationCode, platform);

        String clientId = resolveClientId(platform);
        String clientSecret = appleClientSecretProvider.createClientSecret(clientId);
        AppleTokenResponse tokenResponse = requestToken(
                authorizationCode,
                clientId,
                clientSecret,
                platform
        );

        String token = resolveRevocableToken(tokenResponse);
        String tokenTypeHint = tokenResponse.refreshToken() != null && !tokenResponse.refreshToken().isBlank()
                ? TOKEN_TYPE_HINT_REFRESH_TOKEN
                : TOKEN_TYPE_HINT_ACCESS_TOKEN;

        revokeToken(token, tokenTypeHint, clientId, clientSecret, platform);
    }

    public ApplePublicKeyResponse getPublicKey(){
        try {
            ApplePublicKeyResponse response = webClientBuilder.build()
                    .get()
                    .uri(appleProperties.publicKeyUri())
                    .retrieve()
                    .bodyToMono(ApplePublicKeyResponse.class)
                    .block();

            if (response == null || response.keys() == null || response.keys().isEmpty()) {
                log.warn("Apple public key response is empty");
                throw new BusinessException(ErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
            }

            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (WebClientException | IllegalArgumentException e) {
            log.warn("Failed to get Apple public keys from {}", appleProperties.publicKeyUri(), e);
            throw new BusinessException(ErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        }

    }

    private AppleTokenResponse requestToken(
            String authorizationCode,
            String clientId,
            String clientSecret,
            ApplePlatform platform
    ) {
        try {
            AppleTokenResponse response = webClientBuilder.build()
                    .post()
                    .uri(APPLE_TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters
                            .fromFormData("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("code", authorizationCode)
                            .with("grant_type", GRANT_TYPE_AUTHORIZATION_CODE))
                    .retrieve()
                    .bodyToMono(AppleTokenResponse.class)
                    .block();

            if (response == null
                    || (isBlank(response.accessToken()) && isBlank(response.refreshToken()))) {
                throw new BusinessException(ErrorCode.APPLE_TOKEN_REVOKE_FAILED);
            }

            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (WebClientResponseException exception) {
            logAppleApiFailure(
                    "token",
                    platform,
                    clientId,
                    null,
                    exception
            );
            throw new BusinessException(ErrorCode.APPLE_TOKEN_REVOKE_FAILED);
        } catch (WebClientException | IllegalArgumentException exception) {
            log.warn("Failed to exchange Apple authorization code for revoke. platform={}, clientId={}",
                    platform, clientId, exception);
            throw new BusinessException(ErrorCode.APPLE_TOKEN_REVOKE_FAILED);
        }
    }

    private void revokeToken(
            String token,
            String tokenTypeHint,
            String clientId,
            String clientSecret,
            ApplePlatform platform
    ) {
        try {
            webClientBuilder.build()
                    .post()
                    .uri(APPLE_REVOKE_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters
                            .fromFormData("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("token", token)
                            .with("token_type_hint", tokenTypeHint))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException exception) {
            logAppleApiFailure(
                    "revoke",
                    platform,
                    clientId,
                    tokenTypeHint,
                    exception
            );
            throw new BusinessException(ErrorCode.APPLE_TOKEN_REVOKE_FAILED);
        } catch (WebClientException | IllegalArgumentException exception) {
            log.warn("Failed to revoke Apple token. platform={}, clientId={}, tokenTypeHint={}",
                    platform, clientId, tokenTypeHint, exception);
            throw new BusinessException(ErrorCode.APPLE_TOKEN_REVOKE_FAILED);
        }
    }

    private void logAppleApiFailure(
            String apiName,
            ApplePlatform platform,
            String clientId,
            String tokenTypeHint,
            WebClientResponseException exception
    ) {
        log.warn(
                "Apple {} API failed. status={}, responseBody={}, platform={}, clientId={}, tokenTypeHint={}",
                apiName,
                exception.getStatusCode(),
                exception.getResponseBodyAsString(),
                platform,
                clientId,
                tokenTypeHint
        );
    }

    private String resolveRevocableToken(AppleTokenResponse tokenResponse) {
        if (!isBlank(tokenResponse.refreshToken())) {
            return tokenResponse.refreshToken();
        }
        return tokenResponse.accessToken();
    }

    private String resolveClientId(ApplePlatform platform) {
        return switch (platform) {
            case IOS -> appleProperties.bundleId();
            case ANDROID -> appleProperties.serviceId();
        };
    }

    private void validateAuthorizationCodeRequest(
            String authorizationCode,
            ApplePlatform platform
    ) {
        if (isBlank(authorizationCode) || platform == null) {
            throw new BusinessException(ErrorCode.APPLE_AUTHORIZATION_CODE_REQUIRED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateAudience(Claims claims, String expectedAudience){
        Object audience=claims.get("aud");
        if(audience instanceof String value && expectedAudience.equals(value)){
            return;
        }

        if(audience instanceof Collection<?> values && values.contains(expectedAudience)){
            return;
        }

        throw new BusinessException(ErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
    }

    private void validateNonce(Claims claims, String expectedNonce){
        String actualNonce=claims.get("nonce", String.class);
        if(expectedNonce.equals(actualNonce) || sha256Hex(expectedNonce).equals(actualNonce)){
            return;
        }

        log.warn("Apple nonce mismatch. expected={}, actual={}", expectedNonce, actualNonce);
        throw new BusinessException(ErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
    }

    private String sha256Hex(String value){
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            byte[] hash=digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex=new StringBuilder(hash.length * 2);

            for(byte b : hash){
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        }
    }

    private boolean parseBooleanClaim(Object claim){
        if (claim instanceof Boolean value){
            return value;
        }

        if(claim instanceof String value){
            return Boolean.parseBoolean(value);
        }

        return false;
    }
}
