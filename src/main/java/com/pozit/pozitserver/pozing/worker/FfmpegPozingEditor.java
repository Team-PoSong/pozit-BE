package com.pozit.pozitserver.pozing.worker;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class FfmpegPozingEditor {

    private static final int OUTPUT_WIDTH = 720;
    private static final int OUTPUT_HEIGHT = 1280;
    private static final int OUTPUT_FPS = 30;
    private static final double MIN_SEGMENT_DURATION_SECONDS = 0.1;
    private static final long FFMPEG_TIMEOUT_SECONDS = 120;

    @Value("${pozing.edit.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${pozing.edit.ffprobe-path:ffprobe}")
    private String ffprobePath;

    public Path edit(
            List<PozingEditSegment> segments,
            int memberCount,
            Path workDirectory
    ) {
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Pozing edit segments must not be empty.");
        }
        if (memberCount <= 0) {
            throw new IllegalArgumentException("Pozing edit member count must be positive.");
        }

        int tileHeight = calculateEvenTileHeight(memberCount);
        List<Path> segmentOutputs = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            Path segmentOutput = workDirectory.resolve("segment-%03d.mp4".formatted(i));
            createStackedSegment(segments.get(i), memberCount, tileHeight, segmentOutput);
            segmentOutputs.add(segmentOutput);
        }

        Path result = workDirectory.resolve("result.mp4");
        concatSegments(segmentOutputs, workDirectory.resolve("segments.txt"), result);
        return result;
    }

    private void createStackedSegment(
            PozingEditSegment segment,
            int memberCount,
            int tileHeight,
            Path output
    ) {
        double duration = calculateSegmentDuration(segment.memberVideos());
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y");

        for (Path video : segment.memberVideos()) {
            if (video != null) {
                command.add("-i");
                command.add(video.toString());
            }
        }

        String filterComplex = buildStackFilter(segment.memberVideos(), memberCount, tileHeight, duration);
        command.add("-filter_complex");
        command.add(filterComplex);
        command.add("-map");
        command.add("[outv]");
        command.add("-an");
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("veryfast");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-movflags");
        command.add("+faststart");
        command.add(output.toString());

        run(command);
    }

    private double calculateSegmentDuration(List<Path> memberVideos) {
        double maxDuration = 0;

        for (Path video : memberVideos) {
            if (video == null) {
                continue;
            }
            maxDuration = Math.max(maxDuration, probeDuration(video));
        }

        return Math.max(maxDuration, MIN_SEGMENT_DURATION_SECONDS);
    }

    private String buildStackFilter(
            List<Path> memberVideos,
            int memberCount,
            int tileHeight,
            double duration
    ) {
        StringBuilder filter = new StringBuilder();
        int inputIndex = 0;

        for (int memberIndex = 0; memberIndex < memberCount; memberIndex++) {
            Path video = memberVideos.get(memberIndex);

            if (video == null) {
                filter.append("color=c=black:s=")
                        .append(OUTPUT_WIDTH)
                        .append("x")
                        .append(tileHeight)
                        .append(":r=")
                        .append(OUTPUT_FPS)
                        .append(":d=")
                        .append(formatDuration(duration))
                        .append(",format=yuv420p,setpts=PTS-STARTPTS[v")
                        .append(memberIndex)
                        .append("];");
            } else {
                filter.append("[")
                        .append(inputIndex)
                        .append(":v]scale=")
                        .append(OUTPUT_WIDTH)
                        .append(":")
                        .append(tileHeight)
                        .append(":force_original_aspect_ratio=increase,crop=")
                        .append(OUTPUT_WIDTH)
                        .append(":")
                        .append(tileHeight)
                        .append(",setsar=1,fps=")
                        .append(OUTPUT_FPS)
                        .append(",tpad=stop_mode=clone:stop_duration=")
                        .append(formatDuration(duration))
                        .append(",trim=duration=")
                        .append(formatDuration(duration))
                        .append(",setpts=PTS-STARTPTS[v")
                        .append(memberIndex)
                        .append("];");
                inputIndex++;
            }
        }

        if (memberCount == 1) {
            filter.append("[v0]copy[outv]");
            return filter.toString();
        }

        for (int memberIndex = 0; memberIndex < memberCount; memberIndex++) {
            filter.append("[v").append(memberIndex).append("]");
        }

        filter.append("vstack=inputs=")
                .append(memberCount)
                .append("[outv]");

        return filter.toString();
    }

    private void concatSegments(
            List<Path> segmentOutputs,
            Path concatFile,
            Path result
    ) {
        try {
            List<String> lines = segmentOutputs.stream()
                    .map(path -> "file '" + path.toAbsolutePath() + "'")
                    .toList();
            Files.write(concatFile, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write FFmpeg concat file.", e);
        }

        run(List.of(
                ffmpegPath,
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", concatFile.toString(),
                "-c", "copy",
                result.toString()
        ));
    }

    private double probeDuration(Path video) {
        String output = run(List.of(
                ffprobePath,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                video.toString()
        ));

        return Double.parseDouble(output.trim());
    }

    private String run(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Path outputFile;

        try {
            outputFile=Files.createTempFile("ffmpeg-output-",".log");
            processBuilder.redirectOutput(outputFile.toFile());
            Process process = processBuilder.start();
            boolean finished=process.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if(!finished){
                process.destroyForcibly();
                throw new IllegalStateException("FFmpeg command timed out after %d seconds.".formatted(FFMPEG_TIMEOUT_SECONDS));
            }
            String output=Files.readString(outputFile);
            int exitCode=process.exitValue();

            if (exitCode != 0) {
                throw new IllegalStateException("FFmpeg command failed. exitCode=%d, output=%s"
                        .formatted(exitCode, output));
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute FFmpeg command.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FFmpeg command was interrupted.", e);
        }
    }

    private int calculateEvenTileHeight(int memberCount) {
        return Math.max(2, (OUTPUT_HEIGHT / memberCount) / 2 * 2);
    }

    private String formatDuration(double duration) {
        return String.format(Locale.US, "%.3f", duration);
    }

    public record PozingEditSegment(
            Long courseSpotId,
            List<Path> memberVideos
    ) {
    }
}
