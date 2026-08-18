package com.pozit.pozitserver.global.exception;

import com.pozit.pozitserver.global.alert.DiscordWebhookAlertService;
import com.pozit.pozitserver.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final DiscordWebhookAlertService discordWebhookAlertService;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("Server business exception", e);
            discordWebhookAlertService.sendServerErrorAlert(e, request, errorCode.getHttpStatus().value());
        }

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        ErrorCode.COMMON400.getCode(),
                        "입력값 검증에 실패했습니다.",
                        errors
                ));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(OptimisticLockingFailureException e) {
        return ResponseEntity
                .status(ErrorCode.COMMON409.getHttpStatus())
                .body(ErrorResponse.of(
                        ErrorCode.COMMON409.getCode(),
                        "다른 사용자가 이미 코스를 수정했습니다. 다시 시도해주세요."
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        return ResponseEntity
                .status(ErrorCode.COMMON409.getHttpStatus())
                .body(ErrorResponse.of(
                        ErrorCode.COMMON409.getCode(),
                        ErrorCode.COMMON409.getMessage()
                ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity
                .status(ErrorCode.COMMON404.getHttpStatus())
                .body(ErrorResponse.of(
                        ErrorCode.COMMON404.getCode(),
                        ErrorCode.COMMON404.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception", e);
        discordWebhookAlertService.sendServerErrorAlert(e, request, ErrorCode.COMMON500.getHttpStatus().value());
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(
                        ErrorCode.COMMON500.getCode(),
                        ErrorCode.COMMON500.getMessage()
                ));
    }
}
