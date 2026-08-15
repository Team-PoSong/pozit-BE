package com.pozit.pozitserver.pozing.worker.thumbnail;

import com.pozit.pozitserver.global.s3.S3Service;
import com.pozit.pozitserver.pozing.domain.Pozing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Slf4j
@Component
@RequiredArgsConstructor
public class PozingThumbnailS3Storage {

    private static final String THUMBNAIL_CONTENT_TYPE = "image/jpeg";
    private static final String POZING_PREFIX = "pozings/";
    private static final String THUMBNAIL_PREFIX = "pozing-thumbnails/";

    private final S3Service s3Service;

    public Path createWorkDirectory(Long pozingId) {
        try {
            return Files.createTempDirectory("pozing-thumbnail-" + pozingId + "-");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create pozing thumbnail work directory.", e);
        }
    }

    public Path downloadOriginalVideo(
            Pozing pozing,
            Path target
    ) {
        s3Service.download(pozing.getPozingObjectKey(), target);
        return target;
    }

    public String uploadThumbnail(
            Pozing pozing,
            Path thumbnail
    ) {
        String thumbnailObjectKey = createThumbnailObjectKey(pozing.getPozingObjectKey());
        s3Service.upload(thumbnailObjectKey, thumbnail, THUMBNAIL_CONTENT_TYPE);
        return thumbnailObjectKey;
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
                            log.warn("Failed to delete pozing thumbnail temp file. path={}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to walk pozing thumbnail work directory for cleanup. workDirectory={}", workDirectory, e);
        }
    }

    private String createThumbnailObjectKey(String pozingObjectKey) {
        String keyWithoutPrefix = pozingObjectKey.startsWith(POZING_PREFIX)
                ? pozingObjectKey.substring(POZING_PREFIX.length())
                : pozingObjectKey;

        int lastSlashIndex = keyWithoutPrefix.lastIndexOf('/');
        int lastDotIndex = keyWithoutPrefix.lastIndexOf('.');
        if (lastDotIndex > lastSlashIndex) {
            keyWithoutPrefix = keyWithoutPrefix.substring(0, lastDotIndex);
        }

        return THUMBNAIL_PREFIX + keyWithoutPrefix + ".jpg";
    }
}
