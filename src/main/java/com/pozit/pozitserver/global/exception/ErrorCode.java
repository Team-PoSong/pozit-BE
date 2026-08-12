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
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_1", "유효하지 않은 Refresh Token입니다."),

    // Travel
    INVALID_TRAVEL_PERIOD(HttpStatus.BAD_REQUEST, "TRAVEL400_1", "종료일은 시작일보다 빠를 수 없습니다."),
    TRAVEL_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "TRAVEL400_2", "완료된 여행만 공개 설정을 변경할 수 있습니다."),
    COMPLETED_TRAVEL_DATE_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "TRAVEL400_3", "완료된 여행은 날짜를 수정할 수 없습니다."),
    COMPLETED_TRAVEL_COURSE_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "TRAVEL400_4", "완료된 여행의 코스는 수정할 수 없습니다."),

    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "TRAVEL400_5", "유효하지 않은 초대 코드입니다."),
    ALREADY_JOINED_TRAVEL(HttpStatus.BAD_REQUEST, "TRAVEL400_6", "이미 참여한 여행입니다."),
    CANNOT_JOIN_FINISHED_TRAVEL(HttpStatus.BAD_REQUEST, "TRAVEL400_7", "종료된 여행에는 참여할 수 없습니다."),
    INVALID_SEARCH_PERIOD(HttpStatus.BAD_REQUEST, "TRAVEL400_8", "검색 기간이 올바르지 않습니다."),
    BACKGROUND_IMAGE_UPLOAD_OBJECT_NOT_FOUND(HttpStatus.BAD_REQUEST, "TRAVEL400_9", "S3에 업로드된 배경 사진을 찾을 수 없습니다."),
    CANNOT_LEAVE_AS_LEADER(HttpStatus.BAD_REQUEST, "TRAVEL400_10", "리더는 여행을 나갈 수 없습니다. 여행 삭제를 이용해주세요."),
    CANNOT_REMOVE_SELF(HttpStatus.BAD_REQUEST, "TRAVEL400_11", "본인은 삭제할 수 없습니다."),
    CANNOT_DELETE_COMPLETED_TRAVEL(HttpStatus.BAD_REQUEST, "TRAVEL400_12", "완료된 여행은 삭제할 수 없습니다. 여행 나가기를 이용해주세요."),
    TRAVEL_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAVEL404_1", "해당 여행을 찾을 수 없습니다."),
    TRAVEL_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAVEL404_2", "해당 여행 멤버를 찾을 수 없습니다."),
    CANNOT_DELETE_TRAVEL_WITH_ACTIVE_EDIT_JOB(HttpStatus.CONFLICT, "TRAVEL409_1", "포징 편집 작업이 진행 중이어서 여행을 삭제할 수 없습니다. 잠시 후 다시 시도해주세요."),
    INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "TRAVEL500_1", "초대 코드 생성에 실패했습니다."),

    // Course
    DUPLICATE_COURSE_SPOT(HttpStatus.BAD_REQUEST, "COURSE400_1", "중복된 관광지 ID가 포함되어 있습니다."),
    SEARCHED_TOURIST_SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE404_1", "검색 결과에서 선택한 관광지를 찾을 수 없습니다. 다시 검색해주세요."),
    INVALID_REGION(HttpStatus.BAD_REQUEST,"TRAVEL404_1","존재하지 않는 여행지입니다."),
    COURSE_SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE404_2", "코스 내 해당 여행지를 찾을 수 없습니다."),
    NOT_VALID_TRAVEL_MEMBER(HttpStatus.FORBIDDEN,"COURSE403_1","해당 유저는 이 여행 코스에 접근 권한이 없습니다."),

    // Pozing
    POZING_VIDEO_NOT_FOUND(HttpStatus.BAD_REQUEST, "POZING400_1", "편집할 포징 영상이 없습니다."),
    POZING_UPLOAD_SESSION_NOT_FOUND(HttpStatus.BAD_REQUEST, "POZING400_2", "포징 업로드 요청이 만료되었거나 존재하지 않습니다."),
    POZING_UPLOAD_OBJECT_NOT_FOUND(HttpStatus.BAD_REQUEST, "POZING400_3", "S3에 업로드된 포징 영상을 찾을 수 없습니다."),
    POZING_EDIT_JOB_ALREADY_EXISTS(HttpStatus.CONFLICT, "POZING409_1", "이미 처리 중인 포징 편집 작업이 있습니다."),
    POZING_EDIT_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "POZING404_1", "포징 편집 작업을 찾을 수 없습니다."),
    POZING_EDIT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "POZING500_1", "포징 영상 편집에 실패했습니다."),
    POZING_THUMBNAIL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "POZING500_2", "포징 썸네일 생성에 실패했습니다."),

    // Like
    ALREADY_LIKED(HttpStatus.BAD_REQUEST, "LIKE400_1", "이미 찜한 여행입니다."),
    CANNOT_LIKE_OWN_TRAVEL(HttpStatus.BAD_REQUEST, "LIKE400_2", "본인이 참여한 여행은 찜할 수 없습니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "LIKE404_1", "찜한 여행을 찾을 수 없습니다."),

    //Apple
    INVALID_APPLE_IDENTITY_TOKEN(HttpStatus.UNAUTHORIZED, "APPLELOGIN401_1", "유효하지 않은 Apple identity token입니다."),
    INVALID_APPLE_TOKEN_ISSUE(HttpStatus.UNAUTHORIZED, "APPLELOGIN401_2", "Apple identity token의 issuer가 올바르지 않습니다."),
    NOT_FOUND_APPLE_IDENTITY_TOKEN_SUBJECT(HttpStatus.UNAUTHORIZED, "APPLELOGIN401_3", "Apple identity token에서 회원 고유 식별값을 찾을 수 없습니다."),
    APPLE_AUTHORIZATION_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "APPLELOGIN400_1", "Apple 회원 탈퇴에는 authorizationCode와 platform이 필요합니다."),
    APPLE_TOKEN_REVOKE_FAILED(HttpStatus.BAD_GATEWAY, "APPLELOGIN502_1", "Apple 계정 연동 해제에 실패했습니다."),
    APPLE_CLIENT_SECRET_CONFIG_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "APPLELOGIN500_1", "Apple client secret 설정이 누락되었습니다."),

    //Tour API
    TOUR_API_REQUEST_FAILED(HttpStatus.BAD_GATEWAY,"TOURAPI502_1","관광공사 API 요청에 실패했습니다."),

    // User
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "USER400_1", "이미 사용 중인 닉네임입니다."),

    // Term
    REQUIRED_TERM_NOT_AGREED(HttpStatus.BAD_REQUEST, "TERM400_1", "필수 약관에 모두 동의해야 합니다."),
    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "TERM404_1", "약관 정보를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

}
