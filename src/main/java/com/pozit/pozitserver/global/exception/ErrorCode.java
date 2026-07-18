package com.pozit.pozitserver.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Common
    COMMON400(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    COMMON401(HttpStatus.UNAUTHORIZED, "COMMON401", "인증되지 않은 요청입니다."),
    COMMON403(HttpStatus.FORBIDDEN, "COMMON403", "접근 권한이 없습니다."),
    COMMON404(HttpStatus.NOT_FOUND, "COMMON404", "요청한 리소스를 찾을 수 없습니다."),
    COMMON500(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 내부 오류가 발생했습니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

}