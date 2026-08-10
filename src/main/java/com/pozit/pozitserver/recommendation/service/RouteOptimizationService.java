package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.dto.RecommendedCourseResponse;
import com.pozit.pozitserver.recommendation.model.CourseRecommendCommand;
import com.pozit.pozitserver.recommendation.model.ScoredPlace;
import com.pozit.pozitserver.travel.domain.TravelStyle;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RouteOptimizationService {

    private final StayTimePolicy stayTimePolicy;

    public RouteOptimizationService(StayTimePolicy stayTimePolicy) {
        this.stayTimePolicy = stayTimePolicy;
    }

    public RecommendedCourseResponse createCourse(List<ScoredPlace> places, CourseRecommendCommand command) {
        int dayCount = command.travelDays();
        int placesPerDay = placesPerDay(command.travelStyle());
        int maxPlaceCount = dayCount * placesPerDay;

        List<ScoredPlace> selectedPlaces = places.stream()
                .limit(maxPlaceCount)
                .toList();

        List<List<ScoredPlace>> dayBuckets = distributeByDistance(selectedPlaces, dayCount, placesPerDay);
        List<RecommendedCourseResponse.RecommendedDayResponse> days = new ArrayList<>();

        for (int dayIndex = 0; dayIndex < dayCount; dayIndex++) {
            LocalDate date = command.startDate().plusDays(dayIndex);
            List<ScoredPlace> orderedPlaces = orderByNearestNeighbor(dayBuckets.get(dayIndex));

            List<RecommendedCourseResponse.RecommendedPlaceResponse> placeResponses = new ArrayList<>();
            for (int orderIndex = 0; orderIndex < orderedPlaces.size(); orderIndex++) {
                ScoredPlace scoredPlace = orderedPlaces.get(orderIndex);
                placeResponses.add(new RecommendedCourseResponse.RecommendedPlaceResponse(
                        orderIndex + 1,
                        scoredPlace.place().contentId(),
                        scoredPlace.place().contentTypeId(),
                        scoredPlace.place().title(),
                        scoredPlace.place().address(),
                        scoredPlace.place().imageUrl(),
                        scoredPlace.place().latitude(),
                        scoredPlace.place().longitude(),
                        stayTimePolicy.stayMinutes(scoredPlace.place().contentTypeId()),
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

    private List<List<ScoredPlace>> distributeByDistance(List<ScoredPlace> places, int dayCount, int placesPerDay) {
        List<List<ScoredPlace>> buckets = new ArrayList<>();
        for (int i = 0; i < dayCount; i++) {
            buckets.add(new ArrayList<>());
        }

        List<ScoredPlace> sortedByLongitude = places.stream()
                .sorted(Comparator.comparingDouble(place -> place.place().longitude()))
                .toList();

        int index = 0;
        for (ScoredPlace place : sortedByLongitude) {
            buckets.get(index / placesPerDay).add(place);
            index++;
        }

        return buckets;
    }

    private List<ScoredPlace> orderByNearestNeighbor(List<ScoredPlace> places) {
        if (places.size() <= 2) {
            return places;
        }

        List<ScoredPlace> remaining = new ArrayList<>(places);
        List<ScoredPlace> ordered = new ArrayList<>();

        ScoredPlace current = remaining.stream()
                .max(Comparator.comparingDouble(ScoredPlace::finalScore))
                .orElseThrow();

        ordered.add(current);
        remaining.remove(current);

        while (!remaining.isEmpty()) {
            ScoredPlace base = current;
            current = remaining.stream()
                    .min(Comparator.comparingDouble(candidate -> distance(base, candidate)))
                    .orElseThrow();
            ordered.add(current);
            remaining.remove(current);
        }

        return ordered;
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

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
