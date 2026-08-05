package com.pozit.pozitserver.user.service;

import com.pozit.pozitserver.global.auth.service.AuthTokenService;
import com.pozit.pozitserver.global.auth.service.WithdrawalAccountService;
import com.pozit.pozitserver.global.auth.apple.AppleClient;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.user.domain.SocialProvider;
import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.dto.request.NotificationSettingRequest;
import com.pozit.pozitserver.user.dto.request.UserUpdateRequest;
import com.pozit.pozitserver.user.dto.request.UserWithdrawalRequest;
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
public class UserService {

    private static final String NICKNAME_UNIQUE_CONSTRAINT_NAME = "uk_member_nickname";

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;
    private final AppleClient appleClient;
    private final WithdrawalAccountService withdrawalAccountService;

    @Transactional(readOnly = true)
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

    public void withdraw(
            User user,
            UserWithdrawalRequest request
    ) {
        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMON401);
        }

        revokeAppleAuthorizationIfNeeded(user, request);

        User persistedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON401));
        if (persistedUser.isDeleted()) {
            throw new BusinessException(ErrorCode.COMMON401);
        }

        authTokenService.logoutAllDevices(persistedUser.getId());
        withdrawalAccountService.completeWithdrawal(persistedUser);
    }

    private void revokeAppleAuthorizationIfNeeded(
            User user,
            UserWithdrawalRequest request
    ) {
        if (user.getProvider() != SocialProvider.APPLE) {
            return;
        }

        if (request == null) {
            throw new BusinessException(ErrorCode.APPLE_AUTHORIZATION_CODE_REQUIRED);
        }

        appleClient.revokeAuthorizationCode(
                request.appleAuthorizationCode(),
                request.applePlatform()
        );
    }
}
