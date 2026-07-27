package com.pozit.pozitserver.global.auth.dto.response.apple;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;

import java.util.List;

public record ApplePublicKeyResponse (
        List<ApplePublicKey> keys
){
    public ApplePublicKey getMatchedKey(String kid,String alg){
        return keys.stream()
                .filter(key->key.kid().equals(kid) && key.alg().equals(alg))
                .findAny()
                .orElseThrow(()->new BusinessException(ErrorCode.INVALID_APPLE_IDENTITY_TOKEN));
    }
}
