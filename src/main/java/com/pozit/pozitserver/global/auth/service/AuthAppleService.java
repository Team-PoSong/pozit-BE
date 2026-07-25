package com.pozit.pozitserver.global.auth.service;

import com.pozit.pozitserver.global.auth.apple.AppleJwtParser;
import com.pozit.pozitserver.global.auth.apple.jwt.AppleJwtHeader;
import com.pozit.pozitserver.global.auth.dto.response.apple.ApplePublicKey;
import com.pozit.pozitserver.global.auth.dto.response.apple.ApplePublicKeyResponse;
import com.pozit.pozitserver.global.auth.ios.AppleIdentityTokenRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthAppleService {

    private final WebClient webClient;
    private final AppleJwtParser appleJwtParser;

    public void loginWithAppleIdentityToken(AppleIdentityTokenRequest request){
        String identityToken=request.identityToken();

        // identity token header JWT 디코딩
        AppleJwtHeader header=appleJwtParser.parseHeader(identityToken);

        //apple server의 전체 공개 키 목록 조회
        ApplePublicKeyResponse publicKeys=getPublicKey();

        //헤더의 key, alg와 일치하는 공개키 선택
        ApplePublicKey matchedKey=publicKeys.getMatchedKey(
                header.kid(),
                header.alg()
        );

        //공개키 생성 및 서명 검증

    }

    public ApplePublicKeyResponse getPublicKey(){
        String url="https://appleid.apple.com/auth/keys";
        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(ApplePublicKeyResponse.class)
                .block();

    }
}
