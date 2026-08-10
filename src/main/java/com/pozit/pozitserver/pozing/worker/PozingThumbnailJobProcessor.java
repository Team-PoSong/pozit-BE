package com.pozit.pozitserver.pozing.worker;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.pozing.domain.Pozing;
import com.pozit.pozitserver.pozing.domain.PozingThumbnailStatus;
import com.pozit.pozitserver.pozing.repository.PozingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class PozingThumbnailJobProcessor {

    private final PozingRepository pozingRepository;
    private final PozingThumbnailS3Storage pozingThumbnailS3Storage;
    private final FfmpegPozingThumbnailExtractor ffmpegPozingThumbnailExtractor;
    private final TransactionTemplate transactionTemplate;

    public void process(Long pozingId) {
        PozingTarget target = findTarget(pozingId);
        if (target == null) {
            return;
        }

        Path workDirectory = null;

        try {
            workDirectory = pozingThumbnailS3Storage.createWorkDirectory(pozingId);
            Path video = pozingThumbnailS3Storage.downloadOriginalVideo(target.pozing(), workDirectory.resolve("input.mp4"));
            Path thumbnail = ffmpegPozingThumbnailExtractor.extractFirstFrame(video, workDirectory.resolve("thumbnail.jpg"));
            String thumbnailObjectKey = pozingThumbnailS3Storage.uploadThumbnail(target.pozing(), thumbnail);
            completeThumbnail(pozingId, thumbnailObjectKey);
        } finally {
            pozingThumbnailS3Storage.deleteWorkDirectory(workDirectory);
        }
    }

    private PozingTarget findTarget(Long pozingId) {
        return transactionTemplate.execute(status -> {
            Pozing pozing = pozingRepository.findByIdForUpdate(pozingId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMON404));

            if (pozing.getThumbnailStatus() != null && pozing.getThumbnailStatus() != PozingThumbnailStatus.PENDING) {
                return null;
            }

            return new PozingTarget(pozing);
        });
    }

    private void completeThumbnail(
            Long pozingId,
            String thumbnailObjectKey
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            Pozing pozing = pozingRepository.findByIdForUpdate(pozingId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMON404));

            if (pozing.getThumbnailStatus() == PozingThumbnailStatus.COMPLETED) {
                return;
            }

            pozing.completeThumbnail(thumbnailObjectKey);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markJobFailedInNewTransaction(Long pozingId) {
        Pozing pozing = pozingRepository.findByIdForUpdate(pozingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON404));

        if (pozing.getThumbnailStatus() == PozingThumbnailStatus.COMPLETED) {
            return;
        }

        pozing.failThumbnail();
    }

    private record PozingTarget(Pozing pozing) {
    }
}
