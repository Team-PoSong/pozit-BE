package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.dto.RecommendedCourseResponse;
import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import com.pozit.pozitserver.recommendation.model.CourseRecommendCommand;
import com.pozit.pozitserver.recommendation.model.PlaceFeatureVector;
import com.pozit.pozitserver.recommendation.model.RecommendationTag;
import com.pozit.pozitserver.recommendation.model.ScoredPlace;
import com.pozit.pozitserver.travel.domain.Transportation;
import com.pozit.pozitserver.travel.domain.TravelStyle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RouteOptimizationServiceTest {

    private final RouteOptimizationService routeOptimizationService = new RouteOptimizationService(
            new StayTimePolicy(),
            new OperatingHoursParser()
    );

    @Test
    void createCoursePlacesFoodAtMealSlotBeforeLowerScoreNonFood() {
        RecommendedCourseResponse response = routeOptimizationService.createCourse(
                List.of(
                        scoredPlace("1", "12", "관광지", 37.0, 126.0, "09:00~18:00", null, 0.9),
                        scoredPlace("2", "39", "음식점", 37.001, 126.001, null, "12:00~20:00", 0.7),
                        scoredPlace("3", "12", "낮은 점수 관광지", 37.002, 126.002, "09:00~18:00", null, 0.6)
                ),
                command()
        );

        List<String> titles = response.days().get(0).places().stream()
                .map(RecommendedCourseResponse.RecommendedPlaceResponse::title)
                .toList();

        assertThat(titles).containsSubsequence("관광지", "음식점");
    }

    @Test
    void createCourseExcludesPlaceWhenVisitCannotFitOperatingHours() {
        RecommendedCourseResponse response = routeOptimizationService.createCourse(
                List.of(
                        scoredPlace("1", "12", "닫힌 관광지", 37.0, 126.0, "20:00~21:00", null, 0.9),
                        scoredPlace("2", "12", "열린 관광지", 37.001, 126.001, "09:00~18:00", null, 0.7)
                ),
                command()
        );

        List<String> titles = response.days().get(0).places().stream()
                .map(RecommendedCourseResponse.RecommendedPlaceResponse::title)
                .toList();

        assertThat(titles).contains("열린 관광지");
        assertThat(titles).doesNotContain("닫힌 관광지");
    }

    @Test
    void createCourseAllowsUnknownOperatingHours() {
        RecommendedCourseResponse response = routeOptimizationService.createCourse(
                List.of(scoredPlace("1", "12", "운영시간 미상 관광지", 37.0, 126.0, "전화 문의", null, 0.9)),
                command()
        );

        assertThat(response.days().get(0).places())
                .extracting(RecommendedCourseResponse.RecommendedPlaceResponse::title)
                .containsExactly("운영시간 미상 관광지");
    }

    private CourseRecommendCommand command() {
        return new CourseRecommendCommand(
                null,
                "서울",
                "11",
                LocalDate.of(2026, 9, 11),
                LocalDate.of(2026, 9, 11),
                TravelStyle.NORMAL,
                Transportation.PUBLIC,
                List.of(RecommendationTag.CULTURE)
        );
    }

    private ScoredPlace scoredPlace(
            String contentId,
            String contentTypeId,
            String title,
            double latitude,
            double longitude,
            String useTime,
            String foodOpenTime,
            double finalScore
    ) {
        CandidatePlace place = new CandidatePlace(
                contentId,
                contentTypeId,
                title,
                "서울",
                null,
                Double.toString(longitude),
                Double.toString(latitude),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                useTime,
                null,
                null,
                null,
                null,
                null,
                foodOpenTime,
                null,
                null,
                null,
                null,
                null
        );

        return new ScoredPlace(
                place,
                new PlaceFeatureVector(new double[RecommendationTag.VECTOR_SIZE]),
                finalScore,
                0.5,
                0.5,
                0.5,
                0.5,
                finalScore
        );
    }
}
