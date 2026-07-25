package com.pozit.pozitserver.global.auth.apple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pozit.pozitserver.global.auth.apple.jwt.AppleJwtHeader;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class AppleJwtParser {

    private final ObjectMapper objectMapper;

    public AppleJwtHeader parseHeader(String identityToken){
        validateTokenNotBlank(identityToken);
        String[] tokenParts=identityToken.split("\\.");
        validateTokenStructure(tokenParts);

        try{
            byte[] decodeHeader= Base64.getUrlDecoder()
                    .decode(tokenParts[0]);
            AppleJwtHeader header=objectMapper.readValue(
                    decodeHeader,
                    AppleJwtHeader.class
            );

            validateHeader(header);
            return header;
        }catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_APPLE_IDENTITY_TOKEN
            );
        }
    }

    private void validateTokenNotBlank(String identityToken){
        if (identityToken==null||identityToken.isBlank()){
            throw new BusinessException(
                    ErrorCode.INVALID_APPLE_IDENTITY_TOKEN
            );
        }
    }

    private void validateTokenStructure(String[] tokenParts) {
        if (tokenParts.length != 3) {
            throw new BusinessException(
                    ErrorCode.INVALID_APPLE_IDENTITY_TOKEN
            );
        }

        for (String part : tokenParts) {
            if (part == null || part.isBlank()) {
                throw new BusinessException(
                        ErrorCode.INVALID_APPLE_IDENTITY_TOKEN
                );
            }
        }
    }

    private void validateHeader(AppleJwtHeader header) {
        if (header == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_APPLE_IDENTITY_TOKEN
            );
        }

        if (header.kid() == null || header.kid().isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_APPLE_IDENTITY_TOKEN
            );
        }

        if (!"RS256".equals(header.alg())) {
            throw new BusinessException(
                    ErrorCode.INVALID_APPLE_IDENTITY_TOKEN
            );
        }
    }
}
