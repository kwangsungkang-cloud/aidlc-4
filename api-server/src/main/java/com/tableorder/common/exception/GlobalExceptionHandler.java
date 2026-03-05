package com.tableorder.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR_TYPE_BASE = "https://api.tableorder.com/errors/";

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business exception: code={}, message={}", errorCode.getCode(), e.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                errorCode.getHttpStatus(), e.getMessage());
        problem.setType(URI.create(ERROR_TYPE_BASE + errorCode.getCode().toLowerCase()));
        problem.setTitle(errorCode.getMessage());
        problem.setProperty("code", errorCode.getCode());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "유효하지 않은 값입니다",
                        (first, second) -> first
                ));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다");
        problem.setType(URI.create(ERROR_TYPE_BASE + "validation"));
        problem.setTitle("입력 검증 실패");
        problem.setProperty("code", "COMMON_001");
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "접근 권한이 없습니다");
        problem.setType(URI.create(ERROR_TYPE_BASE + "auth_004"));
        problem.setTitle("접근 거부");
        problem.setProperty("code", "AUTH_004");
        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "파일 크기가 제한을 초과했습니다");
        problem.setType(URI.create(ERROR_TYPE_BASE + "menu_003"));
        problem.setTitle("파일 크기 초과");
        problem.setProperty("code", "MENU_003");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception e) {
        log.error("Unexpected error", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다");
        problem.setType(URI.create(ERROR_TYPE_BASE + "common_003"));
        problem.setTitle("서버 오류");
        problem.setProperty("code", "COMMON_003");
        return problem;
    }
}
