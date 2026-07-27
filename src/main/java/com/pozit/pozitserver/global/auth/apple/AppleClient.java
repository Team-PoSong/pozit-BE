package com.pozit.pozitserver.global.auth.apple;

import com.pozit.pozitserver.global.auth.apple.jwt.AppleJwtHeader;
import com.pozit.pozitserver.global.auth.apple.jwt.AppleTokenClaims;
import com.pozit.pozitserver.global.auth.dto.response.apple.ApplePublicKey;
import com.pozit.pozitserver.global.auth.dto.response.apple.ApplePublicKeyResponse;
import com.pozit.pozitserver.global.auth.ios.AppleIdentityTokenRequest;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.security.PublicKey;
import java.util.Collection;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AppleClient {

    private final AppleJwtProvider appleJwtProvider;
    private final ApplePublicKeyGenerator applePublicKeyGenerator;
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
        if(expectedNonce.equals(actualNonce)){
            return;
        }

        log.warn("Apple nonce mismatch. expected={}, actual={}", expectedNonce, actualNonce);
        throw new BusinessException(ErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
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
