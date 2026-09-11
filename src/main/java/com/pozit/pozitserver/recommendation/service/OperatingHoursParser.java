package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import com.pozit.pozitserver.recommendation.model.OperatingHours;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OperatingHoursParser {

    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile(
            "(?<startHour>\\d{1,2})(?::?(?<startMinute>\\d{2}))?\\s*[~\\-]\\s*"
                    + "(?<endHour>\\d{1,2})(?::?(?<endMinute>\\d{2}))?"
    );

    public OperatingHours parse(CandidatePlace place) {
        String timeText = firstNotBlank(
                isFood(place) ? place.foodOpenTime() : null,
                place.useTime(),
                place.playTime(),
                place.foodOpenTime()
        );
        String restText = firstNotBlank(
                isFood(place) ? place.foodRestDate() : null,
                place.restDate(),
                place.foodRestDate()
        );
        Set<DayOfWeek> closedDays = parseClosedDays(restText);

        if (timeText == null) {
            return OperatingHours.unknown();
        }

        String normalized = normalize(timeText);
        if (containsAny(normalized, "24시간", "상시", "연중무휴", "종일")) {
            return OperatingHours.alwaysOpen(closedDays);
        }

        List<OperatingHours.TimeRange> ranges = parseTimeRanges(normalized);
        if (ranges.isEmpty()) {
            return OperatingHours.unknown();
        }

        return new OperatingHours(false, ranges, closedDays);
    }

    private boolean isFood(CandidatePlace place) {
        return "39".equals(place.contentTypeId());
    }

    private Set<DayOfWeek> parseClosedDays(String restText) {
        if (restText == null) {
            return Set.of();
        }

        String normalized = normalize(restText);
        if (containsAny(normalized, "연중무휴", "없음", "무휴", "상시")) {
            return Set.of();
        }

        EnumSet<DayOfWeek> closedDays = EnumSet.noneOf(DayOfWeek.class);
        addClosedDay(closedDays, normalized, "월", "월요일", DayOfWeek.MONDAY);
        addClosedDay(closedDays, normalized, "화", "화요일", DayOfWeek.TUESDAY);
        addClosedDay(closedDays, normalized, "수", "수요일", DayOfWeek.WEDNESDAY);
        addClosedDay(closedDays, normalized, "목", "목요일", DayOfWeek.THURSDAY);
        addClosedDay(closedDays, normalized, "금", "금요일", DayOfWeek.FRIDAY);
        addClosedDay(closedDays, normalized, "토", "토요일", DayOfWeek.SATURDAY);
        addClosedDay(closedDays, normalized, "일", "일요일", DayOfWeek.SUNDAY);

        return closedDays;
    }

    private void addClosedDay(
            EnumSet<DayOfWeek> closedDays,
            String text,
            String shortName,
            String fullName,
            DayOfWeek dayOfWeek
    ) {
        if (text.contains(fullName) || text.contains("매주" + shortName) || text.contains(shortName + "요일")) {
            closedDays.add(dayOfWeek);
        }
    }

    private List<OperatingHours.TimeRange> parseTimeRanges(String text) {
        Matcher matcher = TIME_RANGE_PATTERN.matcher(text);
        List<OperatingHours.TimeRange> ranges = new ArrayList<>();

        while (matcher.find()) {
            LocalTime startTime = parseTime(matcher.group("startHour"), matcher.group("startMinute"));
            LocalTime endTime = parseTime(matcher.group("endHour"), matcher.group("endMinute"));

            if (startTime == null || endTime == null || startTime.equals(endTime)) {
                continue;
            }

            ranges.add(new OperatingHours.TimeRange(startTime, endTime));
        }

        return ranges;
    }

    private LocalTime parseTime(String hourText, String minuteText) {
        try {
            int hour = Integer.parseInt(hourText);
            int minute = minuteText == null ? 0 : Integer.parseInt(minuteText);

            if (hour == 24 && minute == 0) {
                return LocalTime.MAX;
            }
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }

            return LocalTime.of(hour, minute);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.KOREAN)
                .replace("&nbsp;", " ")
                .replace("∼", "~")
                .replace("〜", "~")
                .replace("－", "-")
                .replace("~", "~")
                .trim();
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
