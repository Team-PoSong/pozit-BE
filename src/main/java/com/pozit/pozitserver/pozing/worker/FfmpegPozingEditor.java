package com.pozit.pozitserver.pozing.worker;

import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class FfmpegPozingEditor {

    private static final int OUTPUT_WIDTH = 720;
    private static final int OUTPUT_HEIGHT = 1280;
    private static final int MAP_AREA_HEIGHT = 480;
    private static final int VIDEO_AREA_HEIGHT = OUTPUT_HEIGHT - MAP_AREA_HEIGHT;
    private static final int OUTPUT_FPS = 30;
    private static final double MIN_SEGMENT_DURATION_SECONDS = 0.1;
    private static final double EMPTY_SEGMENT_DURATION_SECONDS = 3.0;
    private static final long FFMPEG_TIMEOUT_SECONDS = 120;
    private static final long MAP_SCREENSHOT_TIMEOUT_SECONDS = 30;
    private static final String SLEEPING_POZIT_RESOURCE = "static/sleeping_pozit.png";
    private static final String LEGACY_SLEEPING_POZIT_RESOURCE = "static/pozit_sleeping.png";
    private static final String JUST_POSONG_RESOURCE = "static/just_posong.png";
    private static final String LOCATION_RESOURCE = "static/location.png";
    private static final String BADGE_FONT_FAMILY = "Pretendard";
    private static final String BADGE_BOLD_FONT_RESOURCE = "static/Pretendard-1.3.9/public/static/alternative/Pretendard-Bold.ttf";
    private static final String BADGE_SEMIBOLD_FONT_RESOURCE = "static/Pretendard-1.3.9/public/static/alternative/Pretendard-SemiBold.ttf";
    private static final int BADGE_CANVAS_HEIGHT = 140;
    private static final int BADGE_VERTICAL_PADDING = 18;
    private static final int BADGE_HORIZONTAL_PADDING = 34;
    private static final int BADGE_ICON_SIZE = 48;
    private static final int BADGE_DAY_TO_ICON_GAP = 36;
    private static final int BADGE_ICON_TO_NAME_GAP = 10;
    private static final float BADGE_DAY_FONT_SIZE = 30.0f;
    private static final float BADGE_PLACE_FONT_SIZE = 30.0f;
    private static final float BADGE_MIN_PLACE_FONT_SIZE = 24.0f;
    private static final Color MAP_BACKGROUND = new Color(244, 247, 245);
    private static final Color MAP_WATER = new Color(210, 232, 239);
    private static final Color MAP_ROAD = new Color(255, 255, 255, 215);
    private static final Color MAP_ROUTE = new Color(0x9F, 0xA1, 0xFF);
    private static final Color MAP_MARKER_SHADOW = new Color(0x9F, 0xA1, 0xFF, 102);
    private static final Color MAP_MARKER_BACKGROUND = Color.WHITE;
    private static final Color MAP_MARKER_SELECTED_BORDER = new Color(0x9F, 0xA1, 0xFF);
    private static final int MAP_MARKER_SIZE = 40;
    private static final int MAP_MARKER_IMAGE_SIZE = 30;
    private static final float MAP_ROUTE_STROKE_WIDTH = 3.36f;
    private static final float MAP_MARKER_SELECTED_BORDER_WIDTH = 1.5f;
    private static final float MAP_NOT_VISITED_BORDER_WIDTH = 1.0f;
    private static final Color BADGE_DAY_COLOR = new Color(0x66, 0x69, 0xFF);
    private static final Color BADGE_TEXT_COLOR = new Color(0x16, 0x14, 0x24);

    @Value("${pozing.edit.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${pozing.edit.ffprobe-path:ffprobe}")
    private String ffprobePath;

    @Value("${pozing.edit.map-screenshot.enabled:true}")
    private boolean mapScreenshotEnabled;

    @Value("${pozing.edit.map-screenshot.base-url:http://localhost:8080}")
    private String mapScreenshotBaseUrl;

    @Value("${pozing.edit.map-screenshot.chrome-path:}")
    private String configuredChromePath;

    public Path edit(
            Long jobId,
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
            createStackedSegment(jobId, segments.get(i), memberCount, sleepingPozitImage, segmentOutput);
            segmentOutputs.add(segmentOutput);
        }

        Path result = workDirectory.resolve("result.mp4");
        concatSegments(segmentOutputs, workDirectory.resolve("segments.txt"), result);
        return result;
    }

    private void createStackedSegment(
            Long jobId,
            PozingEditSegment segment,
            int memberCount,
            Path sleepingPozitImage,
            Path output
    ) {
        double duration = calculateSegmentDuration(segment.memberVideos());
        List<LayoutCell> videoCells = calculateVideoCells(memberCount);
        Path mapPanelImage = createMapPanelImage(jobId, segment, output);
        List<Path> nicknameImages = createNicknameImages(segment, memberCount, videoCells, output);
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

        for (Path nicknameImage : nicknameImages) {
            if (nicknameImage == null) {
                continue;
            }
            command.add("-loop");
            command.add("1");
            command.add("-t");
            command.add(formatDuration(duration));
            command.add("-i");
            command.add(nicknameImage.toString());
        }

        command.add("-loop");
        command.add("1");
        command.add("-t");
        command.add(formatDuration(duration));
        command.add("-i");
        command.add(mapPanelImage.toString());

        String filterComplex = buildStackFilter(
                segment.memberVideos(),
                nicknameImages,
                memberCount,
                duration
        );
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

    double calculateSegmentDuration(List<Path> memberVideos) {
        double maxDuration = 0;

        for (Path video : memberVideos) {
            if (video == null) {
                continue;
            }
            maxDuration = Math.max(maxDuration, probeDuration(video));
        }

        if (maxDuration == 0) {
            return EMPTY_SEGMENT_DURATION_SECONDS;
        }

        return Math.max(maxDuration, MIN_SEGMENT_DURATION_SECONDS);
    }

    String buildStackFilter(
            List<Path> memberVideos,
            List<Path> nicknameImages,
            int memberCount,
            double duration
    ) {
        StringBuilder filter = new StringBuilder();
        int inputIndex = 0;
        int nicknameInputIndex = memberCount;
        int mapInputIndex = memberCount + (int) nicknameImages.stream().filter(path -> path != null).count();
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

        filter.append("[")
                .append(mapInputIndex)
                .append(":v]scale=")
                .append(OUTPUT_WIDTH)
                .append(":")
                .append(MAP_AREA_HEIGHT)
                .append(":force_original_aspect_ratio=increase,crop=")
                .append(OUTPUT_WIDTH)
                .append(":")
                .append(MAP_AREA_HEIGHT)
                .append(",setsar=1,fps=")
                .append(OUTPUT_FPS)
                .append(",trim=duration=")
                .append(formattedDuration)
                .append(",setpts=PTS-STARTPTS[map];");

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
                        .append("base];");

                nicknameInputIndex = appendNicknameOverlay(
                        filter,
                        nicknameImages.get(memberIndex),
                        nicknameInputIndex,
                        memberIndex,
                        cell,
                        formattedDuration
                );
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
                        .append("base];");

                nicknameInputIndex = appendNicknameOverlay(
                        filter,
                        nicknameImages.get(memberIndex),
                        nicknameInputIndex,
                        memberIndex,
                        cell,
                        formattedDuration
                );
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

    private Path createMapPanelImage(
            Long jobId,
            PozingEditSegment segment,
            Path output
    ) {
        Path imagePath = output.getParent().resolve("map-%03d.png".formatted(segment.courseSpotId()));
        if (captureMapRenderHtml(jobId, segment, imagePath)) {
            drawBadgeOnCapturedMap(imagePath, segment);
            return imagePath;
        }

        BufferedImage image = createMapPanel(segment);

        try {
            ImageIO.write(image, "png", imagePath.toFile());
            return imagePath;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
        }
    }

    private void drawBadgeOnCapturedMap(
            Path imagePath,
            PozingEditSegment segment
    ) {
        try {
            BufferedImage image = ImageIO.read(imagePath.toFile());
            if (image == null) {
                throw new IOException("Captured map image is empty.");
            }

            BufferedImage renderedImage = new BufferedImage(OUTPUT_WIDTH, MAP_AREA_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = renderedImage.createGraphics();
            try {
                applyHighQualityRendering(graphics);
                graphics.drawImage(image, 0, 0, OUTPUT_WIDTH, MAP_AREA_HEIGHT, null);
                drawHeaderPill(graphics, segment.dayNumber(), segment.spotName());
            } finally {
                graphics.dispose();
            }

            ImageIO.write(renderedImage, "png", imagePath.toFile());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
        }
    }

    private boolean captureMapRenderHtml(
            Long jobId,
            PozingEditSegment segment,
            Path imagePath
    ) {
        if (!mapScreenshotEnabled || jobId == null) {
            return false;
        }

        String chromePath = resolveChromePath();
        if (chromePath == null) {
            log.warn("Map screenshot skipped because Chrome/Chromium executable was not found. configuredChromePath={}", configuredChromePath);
            return false;
        }

        String url = "%s/map-render.html?jobId=%d&currentCourseSpotId=%d&capture=true".formatted(
                removeTrailingSlash(mapScreenshotBaseUrl),
                jobId,
                segment.courseSpotId()
        );

        List<String> command = List.of(
                chromePath,
                "--headless=new",
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--hide-scrollbars",
                "--window-size=%d,%d".formatted(OUTPUT_WIDTH, MAP_AREA_HEIGHT),
                "--virtual-time-budget=5000",
                "--screenshot=%s".formatted(imagePath.toAbsolutePath()),
                url
        );

        try {
            runScreenshotCommand(command);
            return Files.exists(imagePath) && Files.size(imagePath) > 0;
        } catch (Exception e) {
            log.warn(
                    "Map screenshot failed. jobId={}, courseSpotId={}, url={}, chromePath={}",
                    jobId,
                    segment.courseSpotId(),
                    url,
                    chromePath,
                    e
            );
            return false;
        }
    }

    private String resolveChromePath() {
        if (configuredChromePath != null && !configuredChromePath.isBlank() && Files.isExecutable(Path.of(configuredChromePath))) {
            return configuredChromePath;
        }

        return Arrays.stream(new String[]{
                        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                        "/usr/bin/google-chrome",
                        "/usr/bin/chromium",
                        "/usr/bin/chromium-browser"
                })
                .filter(path -> Files.isExecutable(Path.of(path)))
                .findFirst()
                .orElse(null);
    }

    private String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private BufferedImage createMapPanel(PozingEditSegment segment) {
        BufferedImage image = new BufferedImage(OUTPUT_WIDTH, MAP_AREA_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            applyHighQualityRendering(graphics);
            drawMapBackground(graphics);
            List<MapPoint> points = calculateMapPoints(segment.routeSpots());
            drawRoute(graphics, points);
            drawSpotMarkers(graphics, points, segment.currentRouteIndex());
            drawHeaderPill(graphics, segment.dayNumber(), segment.spotName());
        } finally {
            graphics.dispose();
        }

        return image;
    }

    private void applyHighQualityRendering(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private void drawMapBackground(Graphics2D graphics) {
        graphics.setColor(MAP_BACKGROUND);
        graphics.fillRect(0, 0, OUTPUT_WIDTH, MAP_AREA_HEIGHT);

        graphics.setColor(MAP_WATER);
        graphics.fillOval(-130, 190, 360, 270);
        graphics.fillOval(460, 165, 310, 240);
        graphics.fillOval(210, 330, 380, 180);

        graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(new Color(222, 226, 224));
        for (int x = -80; x < OUTPUT_WIDTH + 120; x += 85) {
            graphics.drawLine(x, 0, x + 160, MAP_AREA_HEIGHT);
        }
        for (int y = 20; y < MAP_AREA_HEIGHT; y += 78) {
            graphics.drawLine(0, y, OUTPUT_WIDTH, y - 80);
        }

        graphics.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(MAP_ROAD);
        graphics.drawLine(-40, 130, 270, 230);
        graphics.drawLine(240, 225, 760, 145);
        graphics.drawLine(80, 415, 710, 270);

        graphics.setStroke(new BasicStroke(7.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(new Color(255, 230, 112, 160));
        graphics.drawLine(255, -20, 420, 500);
    }

    private List<MapPoint> calculateMapPoints(List<RouteSpot> routeSpots) {
        List<RouteSpot> spotsWithCoordinates = routeSpots.stream()
                .filter(RouteSpot::hasCoordinates)
                .toList();

        if (spotsWithCoordinates.size() != routeSpots.size()) {
            return calculateFallbackMapPoints(routeSpots);
        }

        double minLatitude = routeSpots.stream().mapToDouble(RouteSpot::latitude).min().orElse(0);
        double maxLatitude = routeSpots.stream().mapToDouble(RouteSpot::latitude).max().orElse(0);
        double minLongitude = routeSpots.stream().mapToDouble(RouteSpot::longitude).min().orElse(0);
        double maxLongitude = routeSpots.stream().mapToDouble(RouteSpot::longitude).max().orElse(0);

        double latitudeSpan = Math.max(0.000001, maxLatitude - minLatitude);
        double longitudeSpan = Math.max(0.000001, maxLongitude - minLongitude);
        int paddingX = 90;
        int paddingTop = 125;
        int paddingBottom = 55;
        int drawableWidth = OUTPUT_WIDTH - paddingX * 2;
        int drawableHeight = MAP_AREA_HEIGHT - paddingTop - paddingBottom;

        List<MapPoint> points = new ArrayList<>();
        for (int index = 0; index < routeSpots.size(); index++) {
            RouteSpot spot = routeSpots.get(index);
            double normalizedX = (spot.longitude() - minLongitude) / longitudeSpan;
            double normalizedY = (maxLatitude - spot.latitude()) / latitudeSpan;
            int x = (int) Math.round(paddingX + normalizedX * drawableWidth);
            int y = (int) Math.round(paddingTop + normalizedY * drawableHeight);
            points.add(new MapPoint(x, y, index));
        }

        return points;
    }

    private List<MapPoint> calculateFallbackMapPoints(List<RouteSpot> routeSpots) {
        List<MapPoint> points = new ArrayList<>();
        int count = Math.max(1, routeSpots.size());

        for (int index = 0; index < routeSpots.size(); index++) {
            double progress = count == 1 ? 0.5 : (double) index / (count - 1);
            int x = (int) Math.round(95 + progress * (OUTPUT_WIDTH - 190));
            int y = (int) Math.round(360 - Math.sin(progress * Math.PI) * 170);
            points.add(new MapPoint(x, y, index));
        }

        return points;
    }

    private void drawRoute(Graphics2D graphics, List<MapPoint> points) {
        if (points.size() < 2) {
            return;
        }

        graphics.setColor(new Color(MAP_ROUTE.getRed(), MAP_ROUTE.getGreen(), MAP_ROUTE.getBlue(), 180));
        graphics.setStroke(new BasicStroke(MAP_ROUTE_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int index = 0; index < points.size() - 1; index++) {
            MapPoint start = points.get(index);
            MapPoint end = points.get(index + 1);
            graphics.drawLine(start.x(), start.y(), end.x(), end.y());
        }
    }

    private void drawSpotMarkers(
            Graphics2D graphics,
            List<MapPoint> points,
            int currentRouteIndex
    ) {
        BufferedImage posongImage = readImageResource(JUST_POSONG_RESOURCE);

        for (MapPoint point : points) {
            MapSpotStatus status = resolveMapSpotStatus(point.routeIndex(), currentRouteIndex);

            if (status == MapSpotStatus.VISITED) {
                drawVisitedMarker(graphics, point);
                continue;
            }

            if (status == MapSpotStatus.VISITING) {
                drawPosongMarker(graphics, point, posongImage, true);
                continue;
            }

            drawNotVisitedMarker(graphics, point);
        }
    }

    private MapSpotStatus resolveMapSpotStatus(
            int routeIndex,
            int currentRouteIndex
    ) {
        if (routeIndex < currentRouteIndex) {
            return MapSpotStatus.VISITED;
        }
        if (routeIndex == currentRouteIndex) {
            return MapSpotStatus.VISITING;
        }
        return MapSpotStatus.NOT_VISITED;
    }

    private void drawPosongMarker(
            Graphics2D graphics,
            MapPoint point,
            BufferedImage posongImage,
            boolean selected
    ) {
        drawMarkerBase(graphics, point, MAP_MARKER_BACKGROUND);

        if (posongImage != null) {
            int imageHeight = Math.max(1, posongImage.getHeight() * MAP_MARKER_IMAGE_SIZE / posongImage.getWidth());
            graphics.drawImage(
                    posongImage,
                    point.x() - MAP_MARKER_IMAGE_SIZE / 2,
                    point.y() - imageHeight / 2,
                    MAP_MARKER_IMAGE_SIZE,
                    imageHeight,
                    null
            );
        }

        if (selected) {
            graphics.setStroke(new BasicStroke(MAP_MARKER_SELECTED_BORDER_WIDTH));
            graphics.setColor(MAP_MARKER_SELECTED_BORDER);
            int borderInset = Math.round(MAP_MARKER_SELECTED_BORDER_WIDTH / 2);
            graphics.drawOval(
                    point.x() - MAP_MARKER_SIZE / 2 + borderInset,
                    point.y() - MAP_MARKER_SIZE / 2 + borderInset,
                    MAP_MARKER_SIZE - borderInset * 2,
                    MAP_MARKER_SIZE - borderInset * 2
            );
        }
    }

    private void drawVisitedMarker(Graphics2D graphics, MapPoint point) {
        drawMarkerBase(graphics, point, MAP_MARKER_SELECTED_BORDER);

        graphics.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(Color.WHITE);
        graphics.drawLine(point.x() - 9, point.y(), point.x() - 3, point.y() + 7);
        graphics.drawLine(point.x() - 3, point.y() + 7, point.x() + 10, point.y() - 8);
    }

    private void drawNotVisitedMarker(Graphics2D graphics, MapPoint point) {
        int x = point.x() - MAP_MARKER_SIZE / 2;
        int y = point.y() - MAP_MARKER_SIZE / 2;

        graphics.setColor(Color.WHITE);
        graphics.fillOval(x, y, MAP_MARKER_SIZE, MAP_MARKER_SIZE);
        graphics.setStroke(new BasicStroke(MAP_NOT_VISITED_BORDER_WIDTH));
        graphics.setColor(MAP_MARKER_SELECTED_BORDER);
        graphics.drawOval(x, y, MAP_MARKER_SIZE, MAP_MARKER_SIZE);
    }

    private void drawMarkerBase(
            Graphics2D graphics,
            MapPoint point,
            Color background
    ) {
        int x = point.x() - MAP_MARKER_SIZE / 2;
        int y = point.y() - MAP_MARKER_SIZE / 2;

        graphics.setColor(new Color(
                MAP_MARKER_SHADOW.getRed(),
                MAP_MARKER_SHADOW.getGreen(),
                MAP_MARKER_SHADOW.getBlue(),
                34
        ));
        graphics.fillOval(x - 4, y - 4, MAP_MARKER_SIZE + 8, MAP_MARKER_SIZE + 8);
        graphics.setColor(new Color(
                MAP_MARKER_SHADOW.getRed(),
                MAP_MARKER_SHADOW.getGreen(),
                MAP_MARKER_SHADOW.getBlue(),
                56
        ));
        graphics.fillOval(x - 2, y - 2, MAP_MARKER_SIZE + 4, MAP_MARKER_SIZE + 4);
        graphics.setColor(background);
        graphics.fillOval(x, y, MAP_MARKER_SIZE, MAP_MARKER_SIZE);
    }

    private void drawHeaderPill(
            Graphics2D graphics,
            Integer dayNumber,
            String spotName
    ) {
        String dayText = "Day " + (dayNumber == null ? "-" : dayNumber);
        String nameText = spotName == null || spotName.isBlank() ? "장소" : spotName;
        Font dayFont = createBadgeFont(BADGE_DAY_FONT_SIZE, BADGE_BOLD_FONT_RESOURCE, TextAttribute.WEIGHT_BOLD);

        FontMetrics dayMetrics = graphics.getFontMetrics(dayFont);
        int pillHeight = BADGE_ICON_SIZE + BADGE_VERTICAL_PADDING * 2;
        int maxPillWidth = OUTPUT_WIDTH - 48;
        int fixedWidth = BADGE_HORIZONTAL_PADDING * 2
                + dayMetrics.stringWidth(dayText)
                + BADGE_DAY_TO_ICON_GAP
                + BADGE_ICON_SIZE
                + BADGE_ICON_TO_NAME_GAP;
        int maxNameWidth = Math.max(1, maxPillWidth - fixedWidth);
        Font placeFont = createFittedBadgeFont(
                graphics,
                nameText,
                BADGE_SEMIBOLD_FONT_RESOURCE,
                TextAttribute.WEIGHT_SEMIBOLD,
                maxNameWidth
        );
        FontMetrics placeMetrics = graphics.getFontMetrics(placeFont);
        String fittedNameText = fitText(graphics, nameText, placeFont, maxNameWidth);
        int contentWidth = dayMetrics.stringWidth(dayText)
                + BADGE_DAY_TO_ICON_GAP
                + BADGE_ICON_SIZE
                + BADGE_ICON_TO_NAME_GAP
                + placeMetrics.stringWidth(fittedNameText);
        int pillWidth = Math.min(maxPillWidth, contentWidth + BADGE_HORIZONTAL_PADDING * 2);
        int pillX = (OUTPUT_WIDTH - pillWidth) / 2;
        int pillY = (BADGE_CANVAS_HEIGHT - pillHeight) / 2;

        drawBadgeShadow(graphics, pillX, pillY, pillWidth, pillHeight);
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(pillX, pillY, pillWidth, pillHeight, pillHeight, pillHeight);

        int maxAscent = Math.max(dayMetrics.getAscent(), placeMetrics.getAscent());
        int maxDescent = Math.max(dayMetrics.getDescent(), placeMetrics.getDescent());
        int baseline = pillY + (pillHeight - maxAscent - maxDescent) / 2 + maxAscent;
        int cursor = pillX + BADGE_HORIZONTAL_PADDING;

        graphics.setFont(dayFont);
        graphics.setColor(BADGE_DAY_COLOR);
        graphics.drawString(dayText, cursor, baseline);
        cursor += dayMetrics.stringWidth(dayText) + BADGE_DAY_TO_ICON_GAP;

        drawLocationIcon(graphics, cursor + BADGE_ICON_SIZE / 2, pillY + pillHeight / 2);
        cursor += BADGE_ICON_SIZE + BADGE_ICON_TO_NAME_GAP;

        graphics.setFont(placeFont);
        graphics.setColor(BADGE_TEXT_COLOR);
        graphics.drawString(fittedNameText, cursor, baseline);
    }

    private Font createFittedBadgeFont(
            Graphics2D graphics,
            String text,
            String resourcePath,
            Float weight,
            int maxWidth
    ) {
        float fontSize = BADGE_PLACE_FONT_SIZE;
        Font font = createBadgeFont(fontSize, resourcePath, weight);

        graphics.setFont(font);
        while (fontSize > BADGE_MIN_PLACE_FONT_SIZE
                && graphics.getFontMetrics().stringWidth(text) > maxWidth) {
            fontSize -= 1.0f;
            font = createBadgeFont(fontSize, resourcePath, weight);
            graphics.setFont(font);
        }

        return font;
    }

    private Font createBadgeFont(
            float fontSize,
            String resourcePath,
            Float weight
    ) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (resource.exists()) {
            try {
                return createFontFromResource(resourcePath, fontSize);
            } catch (Exception ignored) {
            }
        }

        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.FAMILY, BADGE_FONT_FAMILY);
        attributes.put(TextAttribute.SIZE, fontSize);
        attributes.put(TextAttribute.WEIGHT, weight);
        return new Font(attributes);
    }

    private Font createFontFromResource(
            String resourcePath,
            float fontSize
    ) throws IOException, FontFormatException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new IOException("Font resource does not exist: " + resourcePath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font.deriveFont(fontSize);
        }
    }

    private void drawBadgeShadow(
            Graphics2D graphics,
            int x,
            int y,
            int width,
            int height
    ) {
        graphics.setColor(new Color(0x7E, 0x7E, 0x7E, 34));
        graphics.fillRoundRect(x - 12, y - 7, width + 24, height + 24, height + 24, height + 24);
        graphics.setColor(new Color(0x7E, 0x7E, 0x7E, 42));
        graphics.fillRoundRect(x - 7, y - 3, width + 14, height + 14, height + 14, height + 14);
        graphics.setColor(new Color(0x7E, 0x7E, 0x7E, 48));
        graphics.fillRoundRect(x - 3, y, width + 6, height + 6, height + 6, height + 6);
    }

    private void drawLocationIcon(Graphics2D graphics, int centerX, int centerY) {
        BufferedImage locationImage = readImageResource(LOCATION_RESOURCE);
        if (locationImage == null) {
            return;
        }

        graphics.drawImage(
                locationImage,
                centerX - BADGE_ICON_SIZE / 2,
                centerY - BADGE_ICON_SIZE / 2,
                BADGE_ICON_SIZE,
                BADGE_ICON_SIZE,
                null
        );
    }

    private BufferedImage readImageResource(String resourcePath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            return null;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
        }
    }

    private String fitText(
            Graphics2D graphics,
            String text,
            Font font,
            int maxWidth
    ) {
        graphics.setFont(font);
        if (graphics.getFontMetrics().stringWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        String fitted = text;
        while (!fitted.isEmpty() && graphics.getFontMetrics().stringWidth(fitted + ellipsis) > maxWidth) {
            fitted = fitted.substring(0, fitted.length() - 1);
        }

        return fitted + ellipsis;
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

    private void runScreenshotCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Path outputFile = Files.createTempFile("map-screenshot-output-", ".log");

        try {
            processBuilder.redirectOutput(outputFile.toFile());
            Process process = processBuilder.start();
            boolean finished = process.waitFor(MAP_SCREENSHOT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                terminate(process);
                throw new IOException("Map screenshot command timed out.");
            }

            if (process.exitValue() != 0) {
                throw new IOException("Map screenshot command failed.");
            }
        } finally {
            Files.deleteIfExists(outputFile);
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

    private List<Path> createNicknameImages(
            PozingEditSegment segment,
            int memberCount,
            List<LayoutCell> videoCells,
            Path output
    ) {
        List<Path> nicknameImages = new ArrayList<>();

        for (int memberIndex = 0; memberIndex < memberCount; memberIndex++) {
            String nickname = segment.memberNicknames().get(memberIndex);
            if (nickname == null || nickname.isBlank()) {
                nicknameImages.add(null);
                continue;
            }

            Path imagePath = output.getParent().resolve(
                    "nickname-%03d-%02d.png".formatted(segment.courseSpotId(), memberIndex)
            );
            writeNicknameImage(nickname, videoCells.get(memberIndex), imagePath);
            nicknameImages.add(imagePath);
        }

        return nicknameImages;
    }

    private void writeNicknameImage(
            String nickname,
            LayoutCell cell,
            Path imagePath
    ) {
        int fontSize = calculateNicknameFontSize(cell);
        int maxWidth = Math.max(1, cell.width() - calculateNicknameMargin(cell) * 2);
        Font font = createNicknameFont(fontSize);

        BufferedImage measureImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measureGraphics = measureImage.createGraphics();
        try {
            measureGraphics.setFont(font);
            FontMetrics metrics = measureGraphics.getFontMetrics();
            while (metrics.stringWidth(nickname) > maxWidth && fontSize > 18) {
                fontSize -= 2;
                font = createNicknameFont(fontSize);
                measureGraphics.setFont(font);
                metrics = measureGraphics.getFontMetrics();
            }
        } finally {
            measureGraphics.dispose();
        }

        BufferedImage labelImage = createNicknameImage(nickname, font);
        try {
            ImageIO.write(labelImage, "png", imagePath.toFile());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.POZING_EDIT_FAILED);
        }
    }

    private BufferedImage createNicknameImage(
            String nickname,
            Font font
    ) {
        BufferedImage measureImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measureGraphics = measureImage.createGraphics();
        FontMetrics metrics;
        try {
            measureGraphics.setFont(font);
            metrics = measureGraphics.getFontMetrics();
        } finally {
            measureGraphics.dispose();
        }

        int width = Math.max(1, metrics.stringWidth(nickname) + 2);
        int height = Math.max(1, metrics.getAscent() + metrics.getDescent() + 2);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setFont(font);
            graphics.setColor(Color.WHITE);
            graphics.drawString(nickname, 1, metrics.getAscent() + 1);
        } finally {
            graphics.dispose();
        }

        return image;
    }

    private Font createNicknameFont(float fontSize) {
        try {
            return createFontFromResource(BADGE_SEMIBOLD_FONT_RESOURCE, fontSize);
        } catch (Exception e) {
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

    private int appendNicknameOverlay(
            StringBuilder filter,
            Path nicknameImage,
            int nicknameInputIndex,
            int memberIndex,
            LayoutCell cell,
            String formattedDuration
    ) {
        if (nicknameImage == null) {
            filter.append("[v")
                    .append(memberIndex)
                    .append("base]copy[v")
                    .append(memberIndex)
                    .append("];");
            return nicknameInputIndex;
        }

        int margin = calculateNicknameMargin(cell);

        filter.append("[")
                .append(nicknameInputIndex)
                .append(":v]format=rgba,fps=")
                .append(OUTPUT_FPS)
                .append(",trim=duration=")
                .append(formattedDuration)
                .append(",setpts=PTS-STARTPTS[label")
                .append(memberIndex)
                .append("];");

        filter.append("[v")
                .append(memberIndex)
                .append("base][label")
                .append(memberIndex)
                .append("]overlay=")
                .append(margin)
                .append(":H-h-")
                .append(margin)
                .append(":format=auto,format=yuv420p[v")
                .append(memberIndex)
                .append("];");

        return nicknameInputIndex + 1;
    }

    private static int calculateNicknameFontSize(LayoutCell cell) {
        return Math.max(20, Math.min(34, cell.height() / 10));
    }

    private static int calculateNicknameMargin(LayoutCell cell) {
        return Math.max(24, Math.min(40, cell.height() / 10));
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
            Integer dayNumber,
            String spotName,
            List<RouteSpot> routeSpots,
            int currentRouteIndex,
            List<Path> memberVideos,
            List<String> memberNicknames
    ) {
    }

    public record RouteSpot(
            Long courseSpotId,
            Integer dayNumber,
            String name,
            Double latitude,
            Double longitude
    ) {

        private boolean hasCoordinates() {
            return latitude != null && longitude != null;
        }
    }

    private record MapPoint(
            int x,
            int y,
            int routeIndex
    ) {
    }

    private enum MapSpotStatus {
        VISITED,
        VISITING,
        NOT_VISITED
    }
}
