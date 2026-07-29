package com.pozit.pozitserver.user.service;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.dto.request.NotificationSettingRequest;
import com.pozit.pozitserver.user.dto.request.UserUpdateRequest;
import com.pozit.pozitserver.user.dto.response.UserInfoResponse;
import com.pozit.pozitserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private static final String NICKNAME_UNIQUE_CONSTRAINT_NAME = "uk_member_nickname";

    private final UserRepository userRepository;

    public UserInfoResponse getMyInfo(User user) {
        return new UserInfoResponse(
                user.getId(),
                user.getNickname(),
                user.getProvider().name(),
                user.getPushEnabled(),
                user.getNotiTravelEnabled(),
                user.getNotiGroupEnabled(),
                user.getNotiPozingEnabled(),
                user.getNotiCourseEnabled(),
                user.getNotiNoticeEnabled()
        );
    }

    @Transactional
    public String makeNewNickname(
            User user,
            UserUpdateRequest request
    ){
        String nickname=request.nickname().trim();
        user.updateProfile(nickname);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (!isNicknameUniqueConstraintViolation(e)) {
                throw e;
            }
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        return nickname;
    }

    private boolean isNicknameUniqueConstraintViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        return cause.getMessage() != null && cause.getMessage().contains(NICKNAME_UNIQUE_CONSTRAINT_NAME);
    }

    @Transactional
    public void updateNotificationSettings(User user, NotificationSettingRequest request) {
        user.updateNotificationSettings(
                request.pushEnabled(),
                request.notiTravelEnabled(),
                request.notiGroupEnabled(),
                request.notiPozingEnabled(),
                request.notiCourseEnabled(),
                request.notiNoticeEnabled()
        );
        userRepository.saveAndFlush(user);
    }
}
