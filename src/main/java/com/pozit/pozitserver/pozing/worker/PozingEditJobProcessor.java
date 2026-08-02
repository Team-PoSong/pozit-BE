package com.pozit.pozitserver.pozing.worker;

import com.pozit.pozitserver.course.domain.CourseSpot;
import com.pozit.pozitserver.course.repository.CourseSpotRepository;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.pozing.domain.PozingEditJob;
import com.pozit.pozitserver.pozing.domain.PozingEditJobStatus;
import com.pozit.pozitserver.pozing.repository.PozingEditJobRepository;
import com.pozit.pozitserver.pozing.repository.PozingRepository;
import com.pozit.pozitserver.travel.domain.TravelMember;
import com.pozit.pozitserver.travel.repository.TravelMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PozingEditJobProcessor {

    private final PozingEditJobRepository pozingEditJobRepository;
    private final PozingRepository pozingRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final TravelMemberRepository travelMemberRepository;
    private final PozingEditS3Storage pozingEditS3Storage;
    private final FfmpegPozingEditor ffmpegPozingEditor;
    private final TransactionTemplate transactionTemplate;

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
            List<TravelMember> members = travelMemberRepository.findAllByTravelIdForEdit(startedJob.travelId());
            List<CourseSpot> courseSpots = courseSpotRepository.findAllByTravelIdForEdit(startedJob.travelId());
            List<Pozing> pozings = pozingRepository.findAllByTravelIdForEdit(startedJob.travelId());

            if (pozings.isEmpty()) {
                throw new BusinessException(ErrorCode.POZING_VIDEO_NOT_FOUND);
            }

            Map<PozingSlot, Path> downloadedVideos = downloadVideosBySlot(pozings, workDirectory);
            List<FfmpegPozingEditor.PozingEditSegment> segments = createEditSegments(
                    courseSpots,
                    members,
                    downloadedVideos
            );

            if (segments.isEmpty()) {
                throw new BusinessException(ErrorCode.POZING_VIDEO_NOT_FOUND);
            }

            Path editedVideo = ffmpegPozingEditor.edit(segments, members.size(), workDirectory);
            String resultS3Key = pozingEditS3Storage.uploadEditedVideo(jobId, editedVideo);
            completeJob(jobId, resultS3Key);
        } finally {
            pozingEditS3Storage.deleteWorkDirectory(workDirectory);
        }
    }

    private Map<PozingSlot, Path> downloadVideosBySlot(
            List<Pozing> pozings,
            Path workDirectory
    ) {
        Map<PozingSlot, Path> downloadedVideos = new HashMap<>();

        for (Pozing pozing : pozings) {
            PozingSlot slot = new PozingSlot(
                    pozing.getCourseSpot().getId(),
                    pozing.getUser().getId()
            );

            if (downloadedVideos.containsKey(slot)) {
                continue;
            }

            Path target = workDirectory.resolve("source-%d.mp4".formatted(pozing.getId()));
            downloadedVideos.put(slot, pozingEditS3Storage.downloadOriginalVideo(pozing, target));
        }

        return downloadedVideos;
    }

    private List<FfmpegPozingEditor.PozingEditSegment> createEditSegments(
            List<CourseSpot> courseSpots,
            List<TravelMember> members,
            Map<PozingSlot, Path> downloadedVideos
    ) {
        List<FfmpegPozingEditor.PozingEditSegment> segments = new ArrayList<>();

        for (CourseSpot courseSpot : courseSpots) {
            List<Path> memberVideos = new ArrayList<>();
            boolean hasAnyVideo = false;

            for (TravelMember member : members) {
                Path video = downloadedVideos.get(new PozingSlot(
                        courseSpot.getId(),
                        member.getUser().getId()
                ));

                if (video != null) {
                    hasAnyVideo = true;
                }

                memberVideos.add(video);
            }

            if (hasAnyVideo) {
                segments.add(new FfmpegPozingEditor.PozingEditSegment(
                        courseSpot.getId(),
                        memberVideos
                ));
            }
        }

        return segments;
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
        });
    }

    @Transactional
    public void fail(Long jobId, String errorMessage) {
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
