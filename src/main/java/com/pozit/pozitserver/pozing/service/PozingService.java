package com.pozit.pozitserver.pozing.service;

import com.pozit.pozitserver.course.domain.Course;
import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.course.domain.CourseSpotStatus;
import com.pozit.pozitserver.course.repository.CourseSpotRepository;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.global.s3.S3Service;
import com.pozit.pozitserver.notification.domain.NotificationType;
import com.pozit.pozitserver.notification.service.NotificationService;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.pozing.domain.PozingEditJob;
import com.pozit.pozitserver.pozing.domain.PozingEditJobStatus;
import com.pozit.pozitserver.pozing.domain.PozingThumbnailStatus;
import com.pozit.pozitserver.pozing.dto.request.PozingSaveRequest;
import com.pozit.pozitserver.pozing.dto.response.PozingEditJobCreateResponse;
import com.pozit.pozitserver.pozing.dto.response.PozingEditJobStatusResponse;
import com.pozit.pozitserver.pozing.dto.response.PozingPresignedUrlResponse;
import com.pozit.pozitserver.pozing.dto.response.PozingSaveResponse;
import com.pozit.pozitserver.pozing.dto.response.PozingThumbnailStatusResponse;
import com.pozit.pozitserver.pozing.repository.PozingEditJobRepository;
import com.pozit.pozitserver.pozing.repository.PozingRepository;
import com.pozit.pozitserver.pozing.worker.PozingEditS3Storage;
import com.pozit.pozitserver.pozing.worker.PozingEditQueuePublisher;
import com.pozit.pozitserver.pozing.worker.PozingThumbnailQueuePublisher;
import com.pozit.pozitserver.travel.domain.Travel;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import com.pozit.pozitserver.travel.repository.TravelRepository;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PozingService {

    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofMinutes(10);
    private static final Duration POZING_GET_URL_EXPIRATION = Duration.ofMinutes(10);
    private static final Duration THUMBNAIL_GET_URL_EXPIRATION = Duration.ofMinutes(10);
    private static final String POZING_VIDEO_CONTENT_TYPE = "video/mp4";

    private final S3Service s3Service;
    private final CourseSpotRepository courseSpotRepository;
    private final PozingRepository pozingRepository;
    private final PozingEditJobRepository pozingEditJobRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final TravelRepository travelRepository;
    private final PozingEditQueuePublisher pozingEditQueuePublisher;
    private final PozingEditS3Storage pozingEditS3Storage;
    private final PozingThumbnailQueuePublisher pozingThumbnailQueuePublisher;
    private final PlatformTransactionManager transactionManager;
    private final NotificationService notificationService;

    /**
     * 타임랩스 저장용 presigned url 발급
     */
    public PozingPresignedUrlResponse getPozingPresignedUrl(User user,Long courseSpotId){
        CourseSpot courseSpot = courseSpotRepository.findById(courseSpotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_SPOT_NOT_FOUND));

        validateMember(courseSpot, user);

        String key = "pozings/%d/%d/%s.mp4".formatted(
                user.getId(),
                courseSpotId,
                UUID.randomUUID()
        );

        var presignedUrl = s3Service.createPutPresignedUrl(
                key,
                POZING_VIDEO_CONTENT_TYPE,
                PRESIGNED_URL_EXPIRATION
        );

        return new PozingPresignedUrlResponse(
                presignedUrl.presignedUrl(),
                key
        );
    }

    /**
     * pozing S3 object key를 DB에 저장
     */
    @Transactional
    public PozingSaveResponse savePozing(
            User user,
            PozingSaveRequest request
    ) {

        CourseSpot courseSpot = courseSpotRepository.findById(request.courseSpotId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_SPOT_NOT_FOUND));

        validateMember(courseSpot, user);

        if (!s3Service.exists(request.objectKey())) {
            throw new BusinessException(ErrorCode.POZING_UPLOAD_OBJECT_NOT_FOUND);
        }

        long savedUserCountBeforeSave = pozingRepository.countDistinctUserByCourseSpot(courseSpot);

        Pozing pozing = pozingRepository.save(
                Pozing.builder()
                        .courseSpot(courseSpot)
                        .user(user)
                        .pozingObjectKey(request.objectKey())
                        .build()
        );

        // 이 장소에 처음 등록된 포징이면 나머지 멤버에게 촬영 유도 알림
        if (savedUserCountBeforeSave == 0) {
            notifyOthersToCapture(courseSpot, user);
        }

        //모든 멤버들이 포징 업데이트 완료 시 VISITED로 상태 변경
        updateCourseSpotStatusIfAllMembersSaved(courseSpot);
        publishThumbnailJobAfterCommit(pozing.getId());

        return new PozingSaveResponse(
                pozing.getId(),
                courseSpot.getId(),
                pozing.getPozingObjectKey(),
                s3Service.createGetPresignedUrl(pozing.getPozingObjectKey(), POZING_GET_URL_EXPIRATION),
                createThumbnailUrl(pozing),
                pozing.getThumbnailStatus()
        );
    }

    @Transactional
    public PozingEditJobCreateResponse requestEditPozing(User user, Long travelId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAVEL_NOT_FOUND));

        validateMember(travel, user);

        if (pozingRepository.countByCourseSpot_Course_Travel(travel) == 0) {
            throw new BusinessException(ErrorCode.POZING_VIDEO_NOT_FOUND);
        }

        boolean hasActiveJob = pozingEditJobRepository.existsByTravelAndStatusIn(
                travel,
                List.of(PozingEditJobStatus.QUEUED, PozingEditJobStatus.PROCESSING)
        );

        if (hasActiveJob) {
            throw new BusinessException(ErrorCode.POZING_EDIT_JOB_ALREADY_EXISTS);
        }

        PozingEditJob job = pozingEditJobRepository.save(PozingEditJob.queued(travel, user));
        publishEditJobAfterCommit(job.getId());

        return new PozingEditJobCreateResponse(job.getId(), job.getStatus());
    }

    public PozingEditJobStatusResponse getEditPozingJob(User user, Long jobId) {
        PozingEditJob job = pozingEditJobRepository.findByIdWithTravel(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POZING_EDIT_JOB_NOT_FOUND));

        validateMember(job.getTravel(), user);

        String downloadUrl = null;
        if (job.getStatus() == PozingEditJobStatus.COMPLETED && job.getResultS3Key() != null) {
            downloadUrl = pozingEditS3Storage.createDownloadUrl(job.getResultS3Key());
        }

        return new PozingEditJobStatusResponse(
                job.getId(),
                job.getStatus(),
                downloadUrl,
                job.getErrorMessage(),
                job.getExpiresAt()
        );
    }

    public PozingThumbnailStatusResponse getThumbnailStatus(User user, Long pozingId) {
        Pozing pozing = pozingRepository.findById(pozingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON404));

        validateMember(pozing.getCourseSpot(), user);

        return new PozingThumbnailStatusResponse(
                pozing.getId(),
                pozing.getThumbnailStatus(),
                createThumbnailUrl(pozing)
        );

    }

    private void validateMember(CourseSpot courseSpot, User user) {
        validateMember(courseSpot.getCourse().getTravel(), user);
    }

    private void validateMember(Travel travel, User user) {
        boolean isMember = travelMemberRepository.existsByTravelAndUser(
                travel,
                user
        );

        if (!isMember) {
            throw new BusinessException(ErrorCode.NOT_VALID_TRAVEL_MEMBER);
        }
    }

    private String createThumbnailUrl(Pozing pozing) {
        if (pozing.getThumbnailStatus() != PozingThumbnailStatus.COMPLETED || pozing.getThumbnailObjectKey() == null) {
            return null;
        }

        return s3Service.createGetPresignedUrl(pozing.getThumbnailObjectKey(), THUMBNAIL_GET_URL_EXPIRATION);
    }

    private void publishEditJobAfterCommit(Long jobId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit(){
                try{
                    pozingEditQueuePublisher.publish(jobId);
                }catch(Exception e){
                    log.error("Failed to publish pozing edit job. JobId={}",jobId);
                    markJobFailedInNewTransaction(jobId, e.getMessage());
                }
            }
        });
    }

    private void publishThumbnailJobAfterCommit(Long pozingId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit(){
                try{
                    pozingThumbnailQueuePublisher.publish(pozingId);
                }catch(Exception e){
                    log.error("Failed to publish pozing thumbnail job. PozingId={}", pozingId, e);
                    try {
                        markThumbnailFailedInNewTransaction(pozingId);
                    } catch (Exception failException) {
                        log.error("Failed to mark pozing thumbnail as failed. PozingId={}", pozingId, failException);
                    }
                }
            }
        });
    }

    private void markThumbnailFailedInNewTransaction(Long pozingId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transactionTemplate.executeWithoutResult(status -> {
            Pozing pozing = pozingRepository.findByIdForUpdate(pozingId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMON404));

            if (pozing.getThumbnailStatus() == PozingThumbnailStatus.COMPLETED) {
                return;
            }

            pozing.failThumbnail();
        });
    }

    private void markJobFailedInNewTransaction(Long jobId, String errorMessage) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transactionTemplate.executeWithoutResult(status -> {
            PozingEditJob job = pozingEditJobRepository.findByIdForUpdate(jobId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.POZING_EDIT_JOB_NOT_FOUND));

            if (job.getStatus() == PozingEditJobStatus.COMPLETED) {
                return;
            }

            job.fail(errorMessage);
        });
    }

    private void notifyOthersToCapture(CourseSpot courseSpot, User registeredUser) {
        Travel travel = courseSpot.getCourse().getTravel();
        List<TravelMember> members = travelMemberRepository.findByTravel(travel);

        for (TravelMember member : members) {
            if (!member.getUser().getId().equals(registeredUser.getId())) {
                notificationService.createNotification(
                        member.getUser(),
                        travel,
                        NotificationType.TRAVEL_LOG,
                        "지금 로그를 촬영해볼까요?"
                );
            }
        }
    }

    private void updateCourseSpotStatusIfAllMembersSaved(CourseSpot courseSpot) {
        long memberCount = travelMemberRepository.countByTravel(
                courseSpot.getCourse().getTravel()
        );
        long savedUserCount = pozingRepository.countDistinctUserByCourseSpot(courseSpot);

        if (memberCount > 0 && savedUserCount >= memberCount) {
            courseSpot.updateStatus(CourseSpotStatus.VISITED);
            notifyCourseCompletedIfAllSpotsVisited(courseSpot);
        }
    }

    private void notifyCourseCompletedIfAllSpotsVisited(CourseSpot courseSpot) {
        Course course = courseSpot.getCourse();
        List<CourseSpot> spots = courseSpotRepository.findByCourseOrderByOrderIndexAsc(course);

        boolean allVisited = spots.stream().allMatch(s -> s.getStatus() == CourseSpotStatus.VISITED);
        if (!allVisited) {
            return;
        }

        Travel travel = course.getTravel();
        List<TravelMember> members = travelMemberRepository.findByTravel(travel);
        for (TravelMember member : members) {
            notificationService.createNotification(
                    member.getUser(),
                    travel,
                    NotificationType.COURSE,
                    "오늘의 코스를 모두 완료했습니다."
            );
        }
    }
}
