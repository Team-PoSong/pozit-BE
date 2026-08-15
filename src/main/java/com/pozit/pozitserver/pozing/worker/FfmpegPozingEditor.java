package com.pozit.pozitserver.pozing.worker;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class FfmpegPozingEditor {

    private static final int OUTPUT_WIDTH = 720;
    private static final int OUTPUT_HEIGHT = 1280;
    private static final int MAP_AREA_HEIGHT = 480;
    private static final int VIDEO_AREA_HEIGHT = OUTPUT_HEIGHT - MAP_AREA_HEIGHT;
    private static final int OUTPUT_FPS = 30;
    private static final double MIN_SEGMENT_DURATION_SECONDS = 0.1;
    private static final long FFMPEG_TIMEOUT_SECONDS = 120;
    private static final String SLEEPING_POZIT_RESOURCE = "static/sleeping_pozit.png";
    private static final String LEGACY_SLEEPING_POZIT_RESOURCE = "static/pozit_sleeping.png";

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

        List<Path> segmentOutputs = new ArrayList<>();
        Path sleepingPozitImage = copySleepingPozitImageIfNeeded(segments, workDirectory);

        for (int i = 0; i < segments.size(); i++) {
            Path segmentOutput = workDirectory.resolve("segment-%03d.mp4".formatted(i));
            createStackedSegment(segments.get(i), memberCount, sleepingPozitImage, segmentOutput);
            segmentOutputs.add(segmentOutput);
        }

        Path result = workDirectory.resolve("result.mp4");
        concatSegments(segmentOutputs, workDirectory.resolve("segments.txt"), result);
        return result;
    }

    private void createStackedSegment(
            PozingEditSegment segment,
            int memberCount,
            Path sleepingPozitImage,
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
            } else {
                command.add("-loop");
                command.add("1");
                command.add("-t");
                command.add(formatDuration(duration));
                command.add("-i");
                command.add(sleepingPozitImage.toString());
            }
        }

        String filterComplex = buildStackFilter(segment.memberVideos(), memberCount, duration);
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
            double duration
    ) {
        StringBuilder filter = new StringBuilder();
        int inputIndex = 0;
        List<LayoutCell> videoCells = calculateVideoCells(memberCount);
        String formattedDuration = formatDuration(duration);

        filter.append("color=c=black:s=")
                .append(OUTPUT_WIDTH)
                .append("x")
                .append(OUTPUT_HEIGHT)
                .append(":r=")
                .append(OUTPUT_FPS)
                .append(":d=")
                .append(formattedDuration)
                .append("[canvas];");

        filter.append("color=c=0xe8efe4:s=")
                .append(OUTPUT_WIDTH)
                .append("x")
                .append(MAP_AREA_HEIGHT)
                .append(":r=")
                .append(OUTPUT_FPS)
                .append(":d=")
                .append(formattedDuration)
                .append(",drawgrid=w=80:h=80:t=1:c=0xc6d7c8@0.35[map];");

        for (int memberIndex = 0; memberIndex < memberCount; memberIndex++) {
            Path video = memberVideos.get(memberIndex);
            LayoutCell cell = videoCells.get(memberIndex);

            if (video == null) {
                int iconSize = calculateSleepingIconSize(cell);
                filter.append("color=c=black:s=")
                        .append(cell.width())
                        .append("x")
                        .append(cell.height())
                        .append(":r=")
                        .append(OUTPUT_FPS)
                        .append(":d=")
                        .append(formattedDuration)
                        .append(",format=yuv420p,setpts=PTS-STARTPTS[empty")
                        .append(memberIndex)
                        .append("];");

                filter.append("[")
                        .append(inputIndex)
                        .append(":v]scale=")
                        .append(iconSize)
                        .append(":")
                        .append(iconSize)
                        .append(":force_original_aspect_ratio=decrease,setsar=1,format=rgba,fps=")
                        .append(OUTPUT_FPS)
                        .append(",trim=duration=")
                        .append(formattedDuration)
                        .append(",setpts=PTS-STARTPTS[icon")
                        .append(memberIndex)
                        .append("];");

                filter.append("[empty")
                        .append(memberIndex)
                        .append("][icon")
                        .append(memberIndex)
                        .append("]overlay=(W-w)/2:(H-h)/2,format=yuv420p[v")
                        .append(memberIndex)
                        .append("];");
                inputIndex++;
            } else {
                filter.append("[")
                        .append(inputIndex)
                        .append(":v]scale=")
                        .append(cell.width())
                        .append(":")
                        .append(cell.height())
                        .append(":force_original_aspect_ratio=increase,crop=")
                        .append(cell.width())
                        .append(":")
                        .append(cell.height())
                        .append(",setsar=1,fps=")
                        .append(OUTPUT_FPS)
                        .append(",tpad=stop_mode=clone:stop_duration=")
                        .append(formattedDuration)
                        .append(",trim=duration=")
                        .append(formattedDuration)
                        .append(",setpts=PTS-STARTPTS[v")
                        .append(memberIndex)
                        .append("];");
                inputIndex++;
            }
        }

        String previousLabel = "base0";
        filter.append("[canvas][map]overlay=0:0[")
                .append(previousLabel)
                .append("];");

        for (int memberIndex = 0; memberIndex < memberCount; memberIndex++) {
            LayoutCell cell = videoCells.get(memberIndex);
            String outputLabel = memberIndex == memberCount - 1 ? "outv" : "base" + (memberIndex + 1);

            filter.append("[")
                    .append(previousLabel)
                    .append("][v")
                    .append(memberIndex)
                    .append("]overlay=")
                    .append(cell.x())
                    .append(":")
                    .append(cell.y())
                    .append("[")
                    .append(outputLabel)
                    .append("]");

            if (memberIndex < memberCount - 1) {
                filter.append(";");
            }

            previousLabel = outputLabel;
        }

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
            throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
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
        Path outputFile = null;

        try {
            outputFile = Files.createTempFile("ffmpeg-output-", ".log");
            processBuilder.redirectOutput(outputFile.toFile());
            Process process = processBuilder.start();
            boolean finished = process.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                terminate(process);
                throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
            }

            String output = Files.readString(outputFile);
            int exitCode = process.exitValue();

            if (exitCode != 0) {
                throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
            }

            return output;

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
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
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
        }
    }

    private Path copySleepingPozitImageIfNeeded(
            List<PozingEditSegment> segments,
            Path workDirectory
    ) {
        boolean hasMissingVideo = segments.stream()
                .flatMap(segment -> segment.memberVideos().stream())
                .anyMatch(video -> video == null);

        if (!hasMissingVideo) {
            return null;
        }

        ClassPathResource resource = new ClassPathResource(SLEEPING_POZIT_RESOURCE);
        if (!resource.exists()) {
            resource = new ClassPathResource(LEGACY_SLEEPING_POZIT_RESOURCE);
        }

        if (!resource.exists()) {
            throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
        }

        Path target = workDirectory.resolve("sleeping_pozit.png");
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
        }
    }

    static List<LayoutCell> calculateVideoCells(int memberCount) {
        if (memberCount <= 0) {
            throw new IllegalArgumentException("Pozing edit member count must be positive.");
        }

        int columns = memberCount >= 4 ? 2 : 1;
        int rows = (int) Math.ceil((double) memberCount / columns);
        int cellWidth = evenFloor(OUTPUT_WIDTH / columns);
        int baseCellHeight = evenFloor(VIDEO_AREA_HEIGHT / rows);

        List<LayoutCell> cells = new ArrayList<>();
        for (int index = 0; index < memberCount; index++) {
            int row = index / columns;
            int column = index % columns;
            int height = row == rows - 1
                    ? VIDEO_AREA_HEIGHT - baseCellHeight * row
                    : baseCellHeight;

            cells.add(new LayoutCell(
                    column * cellWidth,
                    MAP_AREA_HEIGHT + baseCellHeight * row,
                    cellWidth,
                    height
            ));
        }

        return cells;
    }

    private static int evenFloor(int value) {
        return Math.max(2, value / 2 * 2);
    }

    private static int calculateSleepingIconSize(LayoutCell cell) {
        return evenFloor(Math.min(cell.width(), cell.height()) / 2);
    }

    private String formatDuration(double duration) {
        return String.format(Locale.US, "%.3f", duration);
    }

    record LayoutCell(
            int x,
            int y,
            int width,
            int height
    ) {
    }

    public record PozingEditSegment(
            Long courseSpotId,
            List<Path> memberVideos
    ) {
    }
}
