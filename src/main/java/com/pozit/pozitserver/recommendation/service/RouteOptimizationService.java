package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.dto.RecommendedCourseResponse;
import com.pozit.pozitserver.recommendation.model.OperatingHours;
import com.pozit.pozitserver.recommendation.model.CourseRecommendCommand;
import com.pozit.pozitserver.recommendation.model.ScoredPlace;
import com.pozit.pozitserver.travel.domain.Transportation;
import com.pozit.pozitserver.travel.domain.TravelStyle;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RouteOptimizationService {

    private static final LocalTime LUNCH_START_TIME = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END_TIME = LocalTime.of(13, 30);
    private static final LocalTime DINNER_START_TIME = LocalTime.of(18, 0);
    private static final LocalTime DINNER_END_TIME = LocalTime.of(19, 30);

    private final StayTimePolicy stayTimePolicy;
    private final OperatingHoursParser operatingHoursParser;

    public RouteOptimizationService(StayTimePolicy stayTimePolicy, OperatingHoursParser operatingHoursParser) {
        this.stayTimePolicy = stayTimePolicy;
        this.operatingHoursParser = operatingHoursParser;
    }

    public RecommendedCourseResponse createCourse(List<ScoredPlace> places, CourseRecommendCommand command) {
        int dayCount = command.travelDays();
        int placesPerDay = placesPerDay(command.travelStyle());
        List<ScoredPlace> remainingPlaces = new ArrayList<>(places);
        List<RecommendedCourseResponse.RecommendedDayResponse> days = new ArrayList<>();

        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            LocalDate date = command.startDate().plusDays(dayIndex);
            List<ScheduledPlace> scheduledPlaces = createDailySchedule(
                    remainingPlaces,
                    date,
                    placesPerDay,
                    command.travelStyle(),
                    command.transportation()
            );

            List<RecommendedCourseResponse.RecommendedPlaceResponse> placeResponses = new ArrayList<>();
            for (int orderIndex = 0; orderIndex < scheduledPlaces.size(); orderIndex++) {
                ScoredPlace scoredPlace = scheduledPlaces.get(orderIndex).place();
                placeResponses.add(new RecommendedCourseResponse.RecommendedPlaceResponse(
                        orderIndex + 1,
                        scoredPlace.place().contentId(),
                        scoredPlace.place().contentTypeId(),
                        scoredPlace.place().title(),
                        scoredPlace.place().address(),
                        scoredPlace.place().imageUrl(),
                        scoredPlace.place().latitude(),
                        scoredPlace.place().longitude(),
                        scheduledPlaces.get(orderIndex).stayMinutes(),
                        round(scoredPlace.finalScore()),
                        round(scoredPlace.contentScore()),
                        round(scoredPlace.transportationScore()),
                        round(scoredPlace.qualityScore())
                ));
            }

            days.add(new RecommendedCourseResponse.RecommendedDayResponse(
                    dayIndex + 1,
                    date,
                    placeResponses
            ));
        }

        return new RecommendedCourseResponse(command.travelId(), dayCount, days);
    }

    private List<ScheduledPlace> createDailySchedule(
            List<ScoredPlace> remainingPlaces,
            LocalDate date,
            int targetPlaceCount,
            TravelStyle travelStyle,
            Transportation transportation
    ) {
        List<ScheduledPlace> scheduledPlaces = new ArrayList<>();
        DailyTimeWindow timeWindow = dailyTimeWindow(travelStyle);
        ScheduleCursor cursor = new ScheduleCursor(timeWindow.startTime());

        scheduleRegularPlacesUntil(
                remainingPlaces,
                scheduledPlaces,
                cursor,
                date,
                LUNCH_START_TIME,
                targetPlaceCount,
                travelStyle,
                transportation,
                false
        );
        scheduleMealPlace(
                remainingPlaces,
                scheduledPlaces,
                cursor,
                date,
                new TimeSlot(LUNCH_START_TIME, LUNCH_END_TIME),
                targetPlaceCount,
                travelStyle,
                transportation
        );
        scheduleRegularPlacesUntil(
                remainingPlaces,
                scheduledPlaces,
                cursor,
                date,
                hasDinnerSlot(timeWindow) ? DINNER_START_TIME : timeWindow.endTime(),
                targetPlaceCount,
                travelStyle,
                transportation,
                false
        );
        if (hasDinnerSlot(timeWindow)) {
            scheduleMealPlace(
                    remainingPlaces,
                    scheduledPlaces,
                    cursor,
                    date,
                    new TimeSlot(DINNER_START_TIME, DINNER_END_TIME),
                    targetPlaceCount,
                    travelStyle,
                    transportation
            );
        }
        scheduleRegularPlacesUntil(
                remainingPlaces,
                scheduledPlaces,
                cursor,
                date,
                timeWindow.endTime(),
                targetPlaceCount,
                travelStyle,
                transportation,
                true
        );

        return scheduledPlaces;
    }

    private void scheduleRegularPlacesUntil(
            List<ScoredPlace> remainingPlaces,
            List<ScheduledPlace> scheduledPlaces,
            ScheduleCursor cursor,
            LocalDate date,
            LocalTime endTime,
            int targetPlaceCount,
            TravelStyle travelStyle,
            Transportation transportation,
            boolean allowFood
    ) {
        while (scheduledPlaces.size() < targetPlaceCount && cursor.time().isBefore(endTime)) {
            ScheduledPlace nextPlace = findBestFeasiblePlace(
                    remainingPlaces,
                    date,
                    cursor.time(),
                    endTime,
                    lastPlaceOf(scheduledPlaces),
                    travelStyle,
                    transportation,
                    allowFood
            );

            if (nextPlace == null) {
                return;
            }

            scheduledPlaces.add(nextPlace);
            remainingPlaces.remove(nextPlace.place());
            cursor.moveTo(nextPlace.endTime());
        }
    }

    private void scheduleMealPlace(
            List<ScoredPlace> remainingPlaces,
            List<ScheduledPlace> scheduledPlaces,
            ScheduleCursor cursor,
            LocalDate date,
            TimeSlot mealSlot,
            int targetPlaceCount,
            TravelStyle travelStyle,
            Transportation transportation
    ) {
        if (scheduledPlaces.size() >= targetPlaceCount || !cursor.time().isBefore(mealSlot.endTime())) {
            return;
        }

        ScheduledPlace mealPlace = findBestFeasibleMealPlace(
                remainingPlaces,
                date,
                cursor.time(),
                mealSlot,
                lastPlaceOf(scheduledPlaces),
                travelStyle,
                transportation
        );

        if (mealPlace == null) {
            return;
        }

        scheduledPlaces.add(mealPlace);
        remainingPlaces.remove(mealPlace.place());
        cursor.moveTo(mealPlace.endTime());
    }

    private ScheduledPlace findBestFeasiblePlace(
            List<ScoredPlace> remainingPlaces,
            LocalDate date,
            LocalTime earliestStartTime,
            LocalTime latestEndTime,
            ScoredPlace previousPlace,
            TravelStyle travelStyle,
            Transportation transportation,
            boolean allowFood
    ) {
        return remainingPlaces.stream()
                .filter(place -> allowFood || !isFood(place))
                .map(place -> toScheduledPlace(
                        place,
                        date,
                        earliestStartTime,
                        latestEndTime,
                        previousPlace,
                        travelStyle,
                        transportation
                ))
                .filter(scheduledPlace -> scheduledPlace != null)
                .max(Comparator.comparingDouble(scheduledPlace -> schedulingScore(scheduledPlace, previousPlace)))
                .orElse(null);
    }

    private ScheduledPlace findBestFeasibleMealPlace(
            List<ScoredPlace> remainingPlaces,
            LocalDate date,
            LocalTime earliestStartTime,
            TimeSlot mealSlot,
            ScoredPlace previousPlace,
            TravelStyle travelStyle,
            Transportation transportation
    ) {
        return remainingPlaces.stream()
                .filter(this::isFood)
                .map(place -> toMealScheduledPlace(
                        place,
                        date,
                        earliestStartTime,
                        mealSlot,
                        previousPlace,
                        travelStyle,
                        transportation
                ))
                .filter(scheduledPlace -> scheduledPlace != null)
                .max(Comparator.comparingDouble(scheduledPlace -> schedulingScore(scheduledPlace, previousPlace)))
                .orElse(null);
    }

    private ScheduledPlace toScheduledPlace(
            ScoredPlace place,
            LocalDate date,
            LocalTime earliestStartTime,
            LocalTime latestEndTime,
            ScoredPlace previousPlace,
            TravelStyle travelStyle,
            Transportation transportation
    ) {
        int travelMinutes = travelMinutes(previousPlace, place, transportation);
        LocalTime startTime = earliestStartTime.plusMinutes(travelMinutes);
        int stayMinutes = stayTimePolicy.stayMinutes(place.place().contentTypeId(), travelStyle);
        LocalTime endTime = startTime.plusMinutes(stayMinutes);

        if (endTime.isAfter(latestEndTime) || !canVisit(place, date, startTime, endTime)) {
            return null;
        }

        return new ScheduledPlace(place, startTime, endTime, stayMinutes, travelMinutes);
    }

    private ScheduledPlace toMealScheduledPlace(
            ScoredPlace place,
            LocalDate date,
            LocalTime earliestStartTime,
            TimeSlot mealSlot,
            ScoredPlace previousPlace,
            TravelStyle travelStyle,
            Transportation transportation
    ) {
        int travelMinutes = travelMinutes(previousPlace, place, transportation);
        LocalTime arrivalTime = earliestStartTime.plusMinutes(travelMinutes);
        LocalTime startTime = arrivalTime.isAfter(mealSlot.startTime()) ? arrivalTime : mealSlot.startTime();
        int stayMinutes = stayTimePolicy.stayMinutes(place.place().contentTypeId(), travelStyle);
        LocalTime endTime = startTime.plusMinutes(stayMinutes);

        if (endTime.isAfter(mealSlot.endTime()) || !canVisit(place, date, startTime, endTime)) {
            return null;
        }

        return new ScheduledPlace(place, startTime, endTime, stayMinutes, travelMinutes);
    }

    private boolean canVisit(ScoredPlace place, LocalDate date, LocalTime startTime, LocalTime endTime) {
        OperatingHours operatingHours = operatingHoursParser.parse(place.place());
        return operatingHours.canVisit(date, startTime, endTime);
    }

    private double schedulingScore(ScheduledPlace scheduledPlace, ScoredPlace previousPlace) {
        double distancePenalty = previousPlace == null ? 0.0 : distance(previousPlace, scheduledPlace.place()) * 0.03;
        double travelPenalty = scheduledPlace.travelMinutes() * 0.003;
        return scheduledPlace.place().finalScore() - distancePenalty - travelPenalty;
    }

    private int travelMinutes(ScoredPlace source, ScoredPlace target, Transportation transportation) {
        if (source == null) {
            return 0;
        }

        double distanceKm = haversineKm(
                source.place().latitude(),
                source.place().longitude(),
                target.place().latitude(),
                target.place().longitude()
        );
        TravelSpeed travelSpeed = travelSpeed(transportation);

        return (int) Math.ceil((distanceKm / travelSpeed.kilometersPerHour()) * 60.0 + travelSpeed.bufferMinutes());
    }

    private double haversineKm(double sourceLat, double sourceLon, double targetLat, double targetLon) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(targetLat - sourceLat);
        double dLon = Math.toRadians(targetLon - sourceLon);
        double lat1 = Math.toRadians(sourceLat);
        double lat2 = Math.toRadians(targetLat);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private TravelSpeed travelSpeed(Transportation transportation) {
        if (transportation == null) {
            return new TravelSpeed(20.0, 10);
        }

        return switch (transportation) {
            case WALK -> new TravelSpeed(4.0, 5);
            case CAR -> new TravelSpeed(30.0, 10);
            case PUBLIC -> new TravelSpeed(18.0, 15);
        };
    }

    private ScoredPlace lastPlaceOf(List<ScheduledPlace> scheduledPlaces) {
        if (scheduledPlaces.isEmpty()) {
            return null;
        }

        return scheduledPlaces.get(scheduledPlaces.size() - 1).place();
    }

    private boolean isFood(ScoredPlace place) {
        return "39".equals(place.place().contentTypeId());
    }

    private boolean hasDinnerSlot(DailyTimeWindow timeWindow) {
        return !timeWindow.endTime().isBefore(DINNER_END_TIME);
    }

    private double distance(ScoredPlace source, ScoredPlace target) {
        double lat = source.place().latitude() - target.place().latitude();
        double lon = source.place().longitude() - target.place().longitude();
        return lat * lat + lon * lon;
    }

    private int placesPerDay(TravelStyle travelStyle) {
        if (travelStyle == null) {
            return 5;
        }

        return switch (travelStyle) {
            case RELAXED -> 4;
            case NORMAL -> 5;
            case TIGHT -> 7;
        };
    }

    private DailyTimeWindow dailyTimeWindow(TravelStyle travelStyle) {
        if (travelStyle == null) {
            return new DailyTimeWindow(LocalTime.of(10, 0), LocalTime.of(20, 0));
        }

        return switch (travelStyle) {
            case RELAXED -> new DailyTimeWindow(LocalTime.of(10, 30), LocalTime.of(18, 0));
            case NORMAL -> new DailyTimeWindow(LocalTime.of(10, 0), LocalTime.of(20, 0));
            case TIGHT -> new DailyTimeWindow(LocalTime.of(9, 30), LocalTime.of(21, 0));
        };
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private record ScheduledPlace(
            ScoredPlace place,
            LocalTime startTime,
            LocalTime endTime,
            int stayMinutes,
            int travelMinutes
    ) {
    }

    private static class ScheduleCursor {
        private LocalTime time;

        private ScheduleCursor(LocalTime time) {
            this.time = time;
        }

        private LocalTime time() {
            return time;
        }

        private void moveTo(LocalTime time) {
            this.time = time;
        }
    }

    private record TimeSlot(
            LocalTime startTime,
            LocalTime endTime
    ) {
    }

    private record DailyTimeWindow(
            LocalTime startTime,
            LocalTime endTime
    ) {
    }

    private record TravelSpeed(
            double kilometersPerHour,
            int bufferMinutes
    ) {
    }
}
