package com.pozit.pozitserver.global.auth.service;

import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.course.repository.CourseRepository;
import com.pozit.pozitserver.course.repository.CourseSpotRepository;
import com.pozit.pozitserver.global.s3.S3Service;
import com.pozit.pozitserver.like.repository.LikeRepository;
import com.pozit.pozitserver.notification.service.NotificationService;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.pozing.repository.PozingEditJobRepository;
import com.pozit.pozitserver.pozing.repository.PozingRepository;
import com.pozit.pozitserver.pozing.repository.TimelapseManifestRepository;
import com.pozit.pozitserver.support.repository.FeedbackRepository;
import com.pozit.pozitserver.tag.repository.TravelTagRepository;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.travel.domain.TravelMemberRole;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import com.pozit.pozitserver.travel.repository.TravelRepository;
import com.pozit.pozitserver.user.domain.User;
import com.pozit.pozitserver.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalAccountService {

    private final UserRepository userRepository;
    private final TravelRepository travelRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final TravelTagRepository travelTagRepository;
    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final PozingRepository pozingRepository;
    private final PozingEditJobRepository pozingEditJobRepository;
    private final TimelapseManifestRepository timelapseManifestRepository;
    private final LikeRepository likeRepository;
    private final FeedbackRepository feedbackRepository;
    private final S3Service s3Service;
    private final NotificationService notificationService;

    @Transactional
    public void completeWithdrawal(User user) {
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

        objectKeysToDelete.addAll(pozings.stream()
                .map(Pozing::getPozingObjectKey)
                .toList());
        objectKeysToDelete.addAll(pozings.stream()
                .map(Pozing::getThumbnailObjectKey)
                .filter(Objects::nonNull)
                .toList());

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
        notificationService.deleteByTravel(travel);
        travelMemberRepository.deleteAllInBatch(travelMemberRepository.findByTravel(travel));
        timelapseManifestRepository.deleteAllInBatch(timelapseManifestRepository.findByTravel(travel));
        pozingEditJobRepository.deleteAllInBatch(pozingEditJobRepository.findByTravel(travel));
        travelRepository.deleteAllInBatch(List.of(travel));
    }

    private void removeUserOwnedData(User user, List<String> objectKeysToDelete) {
        List<Pozing> pozings = pozingRepository.findByUser(user);
        objectKeysToDelete.addAll(pozings.stream()
                .map(Pozing::getPozingObjectKey)
                .toList());
        objectKeysToDelete.addAll(pozings.stream()
                .map(Pozing::getThumbnailObjectKey)
                .filter(Objects::nonNull)
                .toList());

        pozingRepository.deleteAll(pozings);
        likeRepository.deleteByUser(user);
        feedbackRepository.deleteByUser(user);
        travelMemberRepository.deleteByUser(user);
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
