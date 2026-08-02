package com.pozit.pozitserver.pozing.worker;

import com.pozit.pozitserver.global.s3.S3Service;
import com.pozit.pozitserver.pozing.domain.PozingEditJob;
import com.pozit.pozitserver.pozing.domain.PozingEditJobStatus;
import com.pozit.pozitserver.pozing.repository.PozingEditJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pozing.edit.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class PozingEditResultCleanupScheduler {

    private final PozingEditJobRepository pozingEditJobRepository;
    private final S3Service s3Service;

    @Scheduled(fixedDelayString = "${pozing.edit.cleanup.fixed-delay-ms:60000}")
    @Transactional
    public void cleanupExpiredResults() {
        List<PozingEditJob> expiredJobs = pozingEditJobRepository.findExpiredCompletedJobs(
                PozingEditJobStatus.COMPLETED,
                LocalDateTime.now()
        );

        for (PozingEditJob job : expiredJobs) {
            try {
                s3Service.delete(job.getResultS3Key());
                job.expire();
            } catch (Exception e) {
                log.error("Failed to delete expired pozing edit result. jobId={}, resultS3Key={}",
                        job.getId(),
                        job.getResultS3Key(),
                        e);
            }
        }
    }
}
