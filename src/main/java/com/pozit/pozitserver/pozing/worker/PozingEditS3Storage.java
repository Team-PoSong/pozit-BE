package com.pozit.pozitserver.pozing.worker;

import com.pozit.pozitserver.global.s3.S3Service;
import com.pozit.pozitserver.pozing.domain.Pozing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class PozingEditS3Storage {

    private static final String VIDEO_CONTENT_TYPE = "video/mp4";
    private static final Duration DOWNLOAD_URL_EXPIRATION = Duration.ofMinutes(10);

    private final S3Service s3Service;

    public Path createWorkDirectory(Long jobId) {
        try {
            return Files.createTempDirectory("pozing-edit-" + jobId + "-");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create pozing edit work directory.", e);
        }
    }

    public Path downloadOriginalVideo(
            String objectKey,
            Path target
    ) {
        s3Service.download(objectKey, target);
        return target;
    }

    public String uploadEditedVideo(
            Long jobId,
            Path editedVideo
    ) {
        String resultKey = "pozing-edits/tmp/%d/result.mp4".formatted(jobId);
        s3Service.upload(resultKey, editedVideo, VIDEO_CONTENT_TYPE);
        return resultKey;
    }

    public String createDownloadUrl(String resultS3Key) {
        return s3Service.createGetPresignedUrl(resultS3Key, DOWNLOAD_URL_EXPIRATION);
    }

    public void deleteWorkDirectory(Path workDirectory) {
        if (workDirectory == null || !Files.exists(workDirectory)) {
            return;
        }

        try (var paths = Files.walk(workDirectory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete pozing edit temp file. path={}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to walk pozing edit work directory for cleanup. workDirectory={}", workDirectory, e);
        }
    }
}
