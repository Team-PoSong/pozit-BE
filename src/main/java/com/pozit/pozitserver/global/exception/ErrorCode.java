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
    COMMON409(HttpStatus.CONFLICT, "COMMON409", "요청이 충돌했습니다. 다시 시도해주세요."),
    COMMON500(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 내부 오류가 발생했습니다."),

    // Auth
    KAKAO_INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "KAKAO401", "유효하지 않은 카카오 액세스 토큰입니다."),

    // Travel
    INVALID_TRAVEL_PERIOD(HttpStatus.BAD_REQUEST, "TRAVEL400_1", "종료일은 시작일보다 빠를 수 없습니다."),
    TRAVEL_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "TRAVEL400_2", "완료된 여행만 공개 설정을 변경할 수 있습니다."),
    COMPLETED_TRAVEL_DATE_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "TRAVEL400_3", "완료된 여행은 날짜를 수정할 수 없습니다."),
    COMPLETED_TRAVEL_COURSE_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "TRAVEL400_4", "완료된 여행의 코스는 수정할 수 없습니다."),

    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "TRAVEL400_5", "유효하지 않은 초대 코드입니다."),
    ALREADY_JOINED_TRAVEL(HttpStatus.BAD_REQUEST, "TRAVEL400_6", "이미 참여한 여행입니다."),
    CANNOT_JOIN_FINISHED_TRAVEL(HttpStatus.BAD_REQUEST, "TRAVEL400_7", "종료된 여행에는 참여할 수 없습니다."),
    TRAVEL_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAVEL404_1", "해당 여행을 찾을 수 없습니다."),
    INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "TRAVEL500_1", "초대 코드 생성에 실패했습니다."),

    // Course
    DUPLICATE_COURSE_SPOT(HttpStatus.BAD_REQUEST, "COURSE400_1", "중복된 관광지 ID가 포함되어 있습니다."),
    INVALID_REGION(HttpStatus.BAD_REQUEST,"TRAVEL404_1","존재하지 않는 여행지입니다."),

    //Apple
    INVALID_APPLE_IDENTITY_TOKEN(HttpStatus.UNAUTHORIZED, "APPLELOGIN401_1", "유효하지 않은 Apple identity token입니다."),
    INVALID_APPLE_TOKEN_ISSUE(HttpStatus.UNAUTHORIZED, "APPLELOGIN401_2", "Apple identity token의 issuer가 올바르지 않습니다."),
    NOT_FOUND_APPLE_IDENTITY_TOKEN_SUBJECT(HttpStatus.UNAUTHORIZED, "APPLELOGIN401_3", "Apple identity token에서 회원 고유 식별값을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

}
