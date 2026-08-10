package com.pozit.pozitserver.pozing.worker;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class FfmpegPozingThumbnailExtractor {

    private static final long FFMPEG_TIMEOUT_SECONDS = 30;

    @Value("${pozing.thumbnail.ffmpeg-path:${pozing.edit.ffmpeg-path:ffmpeg}}")
    private String ffmpegPath;

    public Path extractFirstFrame(
            Path video,
            Path thumbnail
    ) {
        run(List.of(
                ffmpegPath,
                "-y",
                "-ss", "0.1",
                "-i", video.toString(),
                "-frames:v", "1",
                "-q:v", "3",
                "-vf", "scale=720:-2",
                thumbnail.toString()
        ));

        return thumbnail;
    }

    private void run(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Path outputFile = null;

        try {
            outputFile = Files.createTempFile("ffmpeg-thumbnail-output-", ".log");
            processBuilder.redirectOutput(outputFile.toFile());
            Process process = processBuilder.start();
            boolean finished = process.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                terminate(process);
                throw new BusinessException(ErrorCode.POZING_THUMBNAIL_FAILED);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new BusinessException(ErrorCode.POZING_THUMBNAIL_FAILED);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.POZING_THUMBNAIL_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.POZING_THUMBNAIL_FAILED);
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void terminate(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }

        process.destroyForcibly();
        try {
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.POZING_THUMBNAIL_FAILED);
        }
    }
}
