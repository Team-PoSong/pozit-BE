package com.pozit.pozitserver.global.auth.apple;

import com.pozit.pozitserver.global.auth.dto.response.apple.ApplePublicKey;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class ApplePublicKeyGenerator {

    /**
     * n,e를 RSA 공개키로 변환
     */
    public PublicKey generatePublicKey(
            ApplePublicKey applePublicKey
    ) {
        try {
            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(applePublicKey.n()));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(applePublicKey.e()));

            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(keySpec);

        } catch (IllegalArgumentException
                 | NoSuchAlgorithmException
                 | InvalidKeySpecException e) {
            throw new BusinessException(
                    ErrorCode.INVALID_APPLE_IDENTITY_TOKEN
            );
        }
    }
}
