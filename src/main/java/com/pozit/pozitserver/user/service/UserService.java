package com.pozit.pozitserver.user.service;

import com.pozit.pozitserver.global.auth.service.AuthTokenService;
import com.pozit.pozitserver.global.auth.apple.AppleClient;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.global.s3.S3Service;
import com.pozit.pozitserver.like.repository.LikeRepository;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.pozing.repository.PozingRepository;
import com.pozit.pozitserver.support.repository.FeedbackRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String NICKNAME_UNIQUE_CONSTRAINT_NAME = "uk_member_nickname";

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;
    private final AppleClient appleClient;
    private final LikeRepository likeRepository;
    private final FeedbackRepository feedbackRepository;
    private final PozingRepository pozingRepository;
    private final S3Service s3Service;
    private final PlatformTransactionManager transactionManager;

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

        withdrawInTransaction(user);
    }

    private void withdrawInTransaction(User user) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            User persistedUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMON401));
            if (persistedUser.isDeleted()) {
                throw new BusinessException(ErrorCode.COMMON401);
            }

            deleteUserData(persistedUser);
        });
    }

    private void deleteUserData(User user) {
        List<Pozing> pozings = pozingRepository.findByUser(user);
        List<String> pozingObjectKeys = pozings.stream()
                .map(Pozing::getPozingObjectKey)
                .toList();

        pozingRepository.deleteAll(pozings);
        likeRepository.deleteByUser(user);
        feedbackRepository.deleteByUser(user);

        authTokenService.logoutAllDevices(user.getId());
        user.withdraw();
        userRepository.saveAndFlush(user);

        deletePozingObjectsAfterCommit(pozingObjectKeys);
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

    private void deletePozingObjectsAfterCommit(List<String> pozingObjectKeys) {
        if (pozingObjectKeys.isEmpty()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pozingObjectKeys.forEach(objectKey -> {
                    try {
                        s3Service.delete(objectKey);
                    } catch (Exception e) {
                        log.error("Failed to delete withdrawn user's pozing object. objectKey={}", objectKey, e);
                    }
                });
            }
        });
    }
}
