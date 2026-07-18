package com.pozit.pozitserver.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean isSuccess,
        String code,
        String message,
        Object result
) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(
                false,
                code,
                message,
                null
        );
    }

    public static ErrorResponse of(String code, String message, Object result) {
        return new ErrorResponse(
                false,
                code,
                message,
                result
        );
    }
}