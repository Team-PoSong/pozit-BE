package com.pozit.pozitserver.user.service;

import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.course.repository.CourseRepository;
import com.pozit.pozitserver.course.repository.CourseSpotRepository;
import com.pozit.pozitserver.global.auth.service.AuthTokenService;
import com.pozit.pozitserver.global.auth.apple.AppleClient;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.global.s3.S3Service;
import com.pozit.pozitserver.like.repository.LikeRepository;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.pozing.repository.PozingRepository;
import com.pozit.pozitserver.support.repository.FeedbackRepository;
import com.pozit.pozitserver.tag.repository.TravelTagRepository;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.travel.domain.TravelMemberRole;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import com.pozit.pozitserver.travel.repository.TravelRepository;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final String NICKNAME_UNIQUE_CONSTRAINT_NAME = "uk_member_nickname";

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;
    private final AppleClient appleClient;
    private final PlatformTransactionManager transactionManager;
    private final TravelRepository travelRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final TravelTagRepository travelTagRepository;
    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final PozingRepository pozingRepository;
    private final LikeRepository likeRepository;
    private final FeedbackRepository feedbackRepository;
    private final S3Service s3Service;

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

        authTokenService.logoutAllDevices(user.getId());
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

            completeWithdrawal(persistedUser);
        });
    }

    private void completeWithdrawal(User user) {
        List<String> objectKeysToDelete = new ArrayList<>();

        handleLeaderTravels(user, objectKeysToDelete);
        removeUserOwnedData(user, objectKeysToDelete);

        user.anonymizeAfterWithdrawal();
        userRepository.saveAndFlush(user);

        deleteS3ObjectsAfterCommit(objectKeysToDelete);
    }

    private void handleLeaderTravels(User user, List<String> objectKeysToDelete) {
        List<Travel> leaderTravels = travelRepository.findByLeader(user);

        for (Travel travel : leaderTravels) {
            List<TravelMember> members = travelMemberRepository.findByTravel(travel);
            List<TravelMember> otherMembers = members.stream()
                    .filter(member -> !member.getUser().getId().equals(user.getId()))
                    .sorted(Comparator.comparing(TravelMember::getJoinedAt).thenComparing(TravelMember::getId))
                    .toList();

            if (otherMembers.isEmpty()) {
                deleteTravel(travel, objectKeysToDelete);
                continue;
            }

            TravelMember nextLeader = otherMembers.get(0);
            nextLeader.changeRole(TravelMemberRole.LEADER);
            travel.transferLeader(nextLeader.getUser());
        }
    }

    private void deleteTravel(Travel travel, List<String> objectKeysToDelete) {
        List<Course> courses = courseRepository.findByTravelOrderByDayNumberAsc(travel);
        List<CourseSpot> courseSpots = courses.isEmpty()
                ? List.of()
                : courseSpotRepository.findAllByCourseInOrder(courses);
        List<Pozing> pozings = courseSpots.isEmpty()
                ? List.of()
                : pozingRepository.findByCourseSpotIn(courseSpots);

        collectPozingObjectKeys(pozings, objectKeysToDelete);

        if (!pozings.isEmpty()) {
            pozingRepository.deleteAllInBatch(pozings);
        }
        if (!courseSpots.isEmpty()) {
            courseSpotRepository.deleteAllInBatch(courseSpots);
        }
        if (!courses.isEmpty()) {
            courseRepository.deleteAllInBatch(courses);
        }

        likeRepository.deleteByTravelIn(List.of(travel));
        travelTagRepository.deleteAllInBatch(travelTagRepository.findByTravel(travel));
        travelMemberRepository.deleteAllInBatch(travelMemberRepository.findByTravel(travel));
        travelRepository.delete(travel);
    }

    private void removeUserOwnedData(User user, List<String> objectKeysToDelete) {
        List<Pozing> pozings = pozingRepository.findByUser(user);
        collectPozingObjectKeys(pozings, objectKeysToDelete);

        pozingRepository.deleteAll(pozings);
        likeRepository.deleteByUser(user);
        feedbackRepository.deleteByUser(user);
        travelMemberRepository.deleteByUser(user);
    }

    private void collectPozingObjectKeys(List<Pozing> pozings, List<String> objectKeysToDelete) {
        objectKeysToDelete.addAll(pozings.stream()
                .map(Pozing::getPozingObjectKey)
                .toList());
        objectKeysToDelete.addAll(pozings.stream()
                .map(Pozing::getThumbnailObjectKey)
                .filter(Objects::nonNull)
                .toList());
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

    private void deleteS3ObjectsAfterCommit(List<String> objectKeys) {
        if (objectKeys.isEmpty()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                objectKeys.forEach(objectKey -> {
                    try {
                        s3Service.delete(objectKey);
                    } catch (Exception e) {
                        log.error("Failed to delete withdrawn user's object. objectKey={}", objectKey, e);
                    }
                });
            }
        });
    }

}
