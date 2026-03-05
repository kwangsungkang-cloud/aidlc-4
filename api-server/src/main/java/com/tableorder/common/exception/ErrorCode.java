package com.tableorder.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "입력값이 올바르지 않습니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_002", "리소스를 찾을 수 없습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_003", "서버 내부 오류가 발생했습니다"),

    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증 정보가 올바르지 않습니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_002", "토큰이 만료되었습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 토큰입니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_004", "접근 권한이 없습니다"),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "AUTH_005", "계정이 잠겨 있습니다"),

    // Store
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE_001", "매장을 찾을 수 없습니다"),

    // Table
    TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "TABLE_001", "테이블을 찾을 수 없습니다"),
    TABLE_ALREADY_EXISTS(HttpStatus.CONFLICT, "TABLE_002", "이미 등록된 테이블 번호입니다"),
    NO_ACTIVE_SESSION(HttpStatus.BAD_REQUEST, "TABLE_003", "활성 세션이 없습니다"),
    SESSION_EXPIRED(HttpStatus.FORBIDDEN, "TABLE_004", "세션이 만료되었습니다"),
    SESSION_CLOSED(HttpStatus.FORBIDDEN, "TABLE_005", "세션이 종료되었습니다"),

    // Order
    EMPTY_ORDER_ITEMS(HttpStatus.BAD_REQUEST, "ORDER_001", "주문 항목이 비어있습니다"),
    MENU_NOT_FOUND(HttpStatus.BAD_REQUEST, "ORDER_002", "메뉴를 찾을 수 없습니다"),
    MENU_STORE_MISMATCH(HttpStatus.BAD_REQUEST, "ORDER_003", "해당 매장의 메뉴가 아닙니다"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_004", "주문을 찾을 수 없습니다"),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "ORDER_005", "유효하지 않은 주문 상태입니다"),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "ORDER_006", "허용되지 않는 상태 전이입니다"),

    // Menu
    CATEGORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "MENU_001", "카테고리를 찾을 수 없습니다"),
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "MENU_002", "지원하지 않는 이미지 형식입니다"),
    IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "MENU_003", "이미지 크기는 5MB 이하여야 합니다"),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MENU_004", "이미지 업로드에 실패했습니다"),

    // Admin
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_001", "관리자를 찾을 수 없습니다"),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "ADMIN_002", "이미 사용 중인 사용자명입니다"),
    INVALID_ACTION_TYPE(HttpStatus.BAD_REQUEST, "ADMIN_003", "유효하지 않은 액션 유형입니다"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "ADMIN_004", "시작일이 종료일보다 클 수 없습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
