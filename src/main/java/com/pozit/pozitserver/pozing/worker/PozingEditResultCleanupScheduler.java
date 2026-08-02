package com.pozit.pozitserver.pozing.worker;

import com.pozit.pozitserver.global.s3.S3Service;
import com.pozit.pozitserver.pozing.domain.PozingEditJob;
import com.pozit.pozitserver.pozing.domain.PozingEditJobStatus;
import com.pozit.pozitserver.pozing.repository.PozingEditJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pozing.edit.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class PozingEditResultCleanupScheduler {

    private final PozingEditJobRepository pozingEditJobRepository;
    private final S3Service s3Service;
    private final TransactionTemplate transactionTemplate;

    @Value("${pozing.edit.processing-timeout-minutes:10}")
    private long processingTimeoutMinutes;

    @Scheduled(fixedDelayString = "${pozing.edit.cleanup.fixed-delay-ms:60000}")
    public void cleanupExpiredResults() {
        failStaleProcessingJobs();

        List<ExpiredJobTarget> expiredJobs = findExpiredJobTargets();

        for (ExpiredJobTarget job : expiredJobs) {
            try {
                s3Service.delete(job.resultS3Key());
                expireJob(job.id());
            } catch (Exception e) {
                log.error("Failed to delete expired pozing edit result. jobId={}, resultS3Key={}",
                        job.id(),
                        job.resultS3Key(),
                        e);
            }
        }
    }

    private List<ExpiredJobTarget> findExpiredJobTargets() {
        return pozingEditJobRepository.findExpiredCompletedJobs(
                        PozingEditJobStatus.COMPLETED,
                        LocalDateTime.now(),
                        PageRequest.of(0, 100)
                )
                .stream()
                .map(job -> new ExpiredJobTarget(job.getId(), job.getResultS3Key()))
                .toList();
    }

    private void expireJob(Long jobId) {
        transactionTemplate.executeWithoutResult(status -> {
            PozingEditJob job = pozingEditJobRepository.findByIdForUpdate(jobId)
                    .orElse(null);

            if (job == null || job.getStatus() != PozingEditJobStatus.COMPLETED) {
                return;
            }

            job.expire();
        });
    }

    private void failStaleProcessingJobs() {
        transactionTemplate.executeWithoutResult(status -> {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(processingTimeoutMinutes);
            List<PozingEditJob> staleJobs = pozingEditJobRepository.findStaleProcessingJobs(
                    PozingEditJobStatus.PROCESSING,
                    threshold
            );

            for (PozingEditJob job : staleJobs) {
                job.fail("Pozing edit job timed out while PROCESSING.");
            }
        });
    }

    private record ExpiredJobTarget(
            Long id,
            String resultS3Key
    ) {
    }
}
