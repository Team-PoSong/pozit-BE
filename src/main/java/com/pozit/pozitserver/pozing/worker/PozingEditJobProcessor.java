package com.pozit.pozitserver.pozing.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.notification.domain.NotificationType;
import com.pozit.pozitserver.notification.service.NotificationService;
import com.pozit.pozitserver.pozing.domain.PozingEditJob;
import com.pozit.pozitserver.pozing.domain.PozingEditJobStatus;
import com.pozit.pozitserver.pozing.domain.TimelapseManifest;
import com.pozit.pozitserver.pozing.model.TimelapseManifestPayload;
import com.pozit.pozitserver.pozing.repository.PozingEditJobRepository;
import com.pozit.pozitserver.pozing.repository.TimelapseManifestRepository;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PozingEditJobProcessor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PozingEditJobRepository pozingEditJobRepository;
    private final TimelapseManifestRepository timelapseManifestRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final PozingEditS3Storage pozingEditS3Storage;
    private final FfmpegPozingEditor ffmpegPozingEditor;
    private final TransactionTemplate transactionTemplate;
    private final NotificationService notificationService;

    @Value("${pozing.edit.result-expiration-minutes:10}")
    private long resultExpirationMinutes;

    public void process(Long jobId) {
        StartedJob startedJob = startJob(jobId);
        if (startedJob == null) {
            return;
        }

        Path workDirectory = null;

        try {
            workDirectory = pozingEditS3Storage.createWorkDirectory(jobId);
            TimelapseManifestPayload manifest = loadManifest(jobId);
            int memberCount = countMembers(manifest);
            Map<PozingSlot, Path> downloadedVideos = downloadVideosBySlot(manifest, workDirectory);
            List<FfmpegPozingEditor.PozingEditSegment> segments = createEditSegments(
                    manifest,
                    downloadedVideos
            );

            if (segments.isEmpty()) {
                throw new BusinessException(ErrorCode.POZING_VIDEO_NOT_FOUND);
            }

            Path editedVideo = ffmpegPozingEditor.edit(segments, memberCount, workDirectory);
            String resultS3Key = pozingEditS3Storage.uploadEditedVideo(jobId, editedVideo);
            completeJob(jobId, resultS3Key);
        } finally {
            pozingEditS3Storage.deleteWorkDirectory(workDirectory);
        }
    }

    private TimelapseManifestPayload loadManifest(Long jobId) {
        TimelapseManifest manifest = timelapseManifestRepository.findByPozingEditJob_Id(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POZING_EDIT_FAILED));

        try {
            return OBJECT_MAPPER.readValue(
                    manifest.getManifestJson(),
                    TimelapseManifestPayload.class
            );
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
        }
    }

    private int countMembers(TimelapseManifestPayload manifest) {
        return manifest.courses().stream()
                .flatMap(course -> course.spots().stream())
                .findFirst()
                .map(spot -> spot.pozings().size())
                .filter(count -> count > 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.POZING_VIDEO_NOT_FOUND));
    }

    private Map<PozingSlot, Path> downloadVideosBySlot(
            TimelapseManifestPayload manifest,
            Path workDirectory
    ) {
        Map<PozingSlot, Path> downloadedVideos = new HashMap<>();
        int sourceIndex = 0;

        for (TimelapseManifestPayload.CourseManifest course : manifest.courses()) {
            for (TimelapseManifestPayload.SpotManifest spot : course.spots()) {
                for (TimelapseManifestPayload.MemberPozingManifest pozing : spot.pozings()) {
                    if (pozing.pozingObjectKey() == null) {
                        continue;
                    }

                    PozingSlot slot = new PozingSlot(
                            spot.courseSpotId(),
                            pozing.userId()
                    );

                    if (downloadedVideos.containsKey(slot)) {
                        continue;
                    }

                    Path target = workDirectory.resolve("source-%03d.mp4".formatted(sourceIndex++));
                    downloadedVideos.put(
                            slot,
                            pozingEditS3Storage.downloadOriginalVideo(pozing.pozingObjectKey(), target)
                    );
                }
            }
        }

        return downloadedVideos;
    }

    private List<FfmpegPozingEditor.PozingEditSegment> createEditSegments(
            TimelapseManifestPayload manifest,
            Map<PozingSlot, Path> downloadedVideos
    ) {
        List<FfmpegPozingEditor.PozingEditSegment> segments = new ArrayList<>();
        List<FfmpegPozingEditor.RouteSpot> routeSpots = createRouteSpots(manifest);
        int routeIndex = 0;

        for (TimelapseManifestPayload.CourseManifest course : manifest.courses()) {
            for (TimelapseManifestPayload.SpotManifest spot : course.spots()) {
                List<Path> memberVideos = new ArrayList<>();
                List<String> memberNicknames = new ArrayList<>();
                boolean hasAnyVideo = false;

                for (TimelapseManifestPayload.MemberPozingManifest pozing : spot.pozings()) {
                    Path video = downloadedVideos.get(new PozingSlot(
                            spot.courseSpotId(),
                            pozing.userId()
                    ));

                    if (video != null) {
                        hasAnyVideo = true;
                    }

                    memberVideos.add(video);
                    memberNicknames.add(pozing.nickname());
                }

                if (hasAnyVideo) {
                    segments.add(new FfmpegPozingEditor.PozingEditSegment(
                            spot.courseSpotId(),
                            course.dayNumber(),
                            spot.name(),
                            routeSpots,
                            routeIndex,
                            memberVideos,
                            memberNicknames
                    ));
                }

                routeIndex++;
            }
        }

        return segments;
    }

    private List<FfmpegPozingEditor.RouteSpot> createRouteSpots(TimelapseManifestPayload manifest) {
        List<FfmpegPozingEditor.RouteSpot> routeSpots = new ArrayList<>();

        for (TimelapseManifestPayload.CourseManifest course : manifest.courses()) {
            for (TimelapseManifestPayload.SpotManifest spot : course.spots()) {
                routeSpots.add(new FfmpegPozingEditor.RouteSpot(
                        spot.courseSpotId(),
                        course.dayNumber(),
                        spot.name(),
                        toDouble(spot.latitude()),
                        toDouble(spot.longitude())
                ));
            }
        }

        return routeSpots;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private StartedJob startJob(Long jobId) {
        return transactionTemplate.execute(status -> {
            PozingEditJob job = pozingEditJobRepository.findByIdForUpdate(jobId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.POZING_EDIT_JOB_NOT_FOUND));

            if (job.getStatus() != PozingEditJobStatus.QUEUED) {
                return null;
            }

            job.start();
            return new StartedJob(job.getId(), job.getTravel().getId());
        });
    }

    private void completeJob(Long jobId, String resultS3Key) {
        transactionTemplate.executeWithoutResult(status -> {
            PozingEditJob job = pozingEditJobRepository.findByIdForUpdate(jobId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.POZING_EDIT_JOB_NOT_FOUND));

            job.complete(resultS3Key, LocalDateTime.now().plusMinutes(resultExpirationMinutes));

            List<TravelMember> members = travelMemberRepository.findByTravel(job.getTravel());
            for (TravelMember member : members) {
                notificationService.createNotification(
                        member.getUser(),
                        job.getTravel(),
                        NotificationType.TIMELAPSE,
                        "여행 로그가 생성되었습니다."
                );
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markJobFailedInNewTransaction(Long jobId, String errorMessage) {
        PozingEditJob job = pozingEditJobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POZING_EDIT_JOB_NOT_FOUND));

        if (job.getStatus() == PozingEditJobStatus.COMPLETED) {
            return;
        }

        job.fail(errorMessage);
    }

    private record StartedJob(
            Long jobId,
            Long travelId
    ) {
    }

    private record PozingSlot(
            Long courseSpotId,
            Long userId
    ) {
    }
}
