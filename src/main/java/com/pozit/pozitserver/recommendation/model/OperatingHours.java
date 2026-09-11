package com.pozit.pozitserver.recommendation.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public record OperatingHours(
        boolean isUnknown,
        List<TimeRange> openRanges,
        Set<DayOfWeek> closedDays
) {

    public static OperatingHours unknown() {
        return new OperatingHours(true, List.of(), Set.of());
    }

    public static OperatingHours alwaysOpen(Set<DayOfWeek> closedDays) {
        return new OperatingHours(false, List.of(new TimeRange(LocalTime.MIN, LocalTime.MAX)), closedDays);
    }

    public boolean canVisit(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (isUnknown) {
            return true;
        }
        if (closedDays.contains(date.getDayOfWeek())) {
            return false;
        }
        if (openRanges.isEmpty()) {
            return true;
        }

        return openRanges.stream()
                .anyMatch(range -> range.contains(startTime, endTime));
    }

    public record TimeRange(
            LocalTime startTime,
            LocalTime endTime
    ) {
        public boolean contains(LocalTime visitStartTime, LocalTime visitEndTime) {
            if (!endTime.isBefore(startTime)) {
                return !visitStartTime.isBefore(startTime) && !visitEndTime.isAfter(endTime);
            }

            return !visitStartTime.isBefore(startTime) || !visitEndTime.isAfter(endTime);
        }
    }
}
