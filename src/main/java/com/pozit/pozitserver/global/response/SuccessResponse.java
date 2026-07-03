package com.pozit.pozitserver.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuccessResponse<T> (
        boolean isSuccess,
        String code,
        String message,
        T result
){

    //success
    public static <T> SuccessResponse<T> ok(T result) {
        return new SuccessResponse<>(
                true,
                "COMMON200",
                "요청에 성공했습니다.",
                result
        );
    }

    public static SuccessResponse<Void> ok() {
        return new SuccessResponse<>(
                true,
                "COMMON200",
                "요청에 성공했습니다.",
                null
        );
    }

}
