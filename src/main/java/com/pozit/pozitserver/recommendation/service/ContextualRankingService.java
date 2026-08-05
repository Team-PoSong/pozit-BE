package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.model.CourseRecommendCommand;
import com.pozit.pozitserver.recommendation.model.ScoredPlace;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ContextualRankingService {

    private static final double NEUTRAL_CONGESTION_SCORE = 0.5;
    private static final double NEUTRAL_REGION_TREND_SCORE = 0.5;

    private final TransportationScoreCalculator transportationScoreCalculator;
    private final PlaceQualityScoreCalculator placeQualityScoreCalculator;

    public ContextualRankingService(
            TransportationScoreCalculator transportationScoreCalculator,
            PlaceQualityScoreCalculator placeQualityScoreCalculator
    ) {
        this.transportationScoreCalculator = transportationScoreCalculator;
        this.placeQualityScoreCalculator = placeQualityScoreCalculator;
    }

    public List<ScoredPlace> rank(List<ScoredPlace> scoredPlaces, CourseRecommendCommand command) {
        return scoredPlaces.stream()
                .map(scoredPlace -> applyContext(scoredPlace, command))
                .sorted(Comparator.comparingDouble(ScoredPlace::finalScore).reversed())
                .toList();
    }

    private ScoredPlace applyContext(ScoredPlace scoredPlace, CourseRecommendCommand command) {
        double congestionScore = NEUTRAL_CONGESTION_SCORE;
        double regionTrendScore = NEUTRAL_REGION_TREND_SCORE;
        double transportationScore = transportationScoreCalculator.calculate(scoredPlace.place(), command.transportation());
        double qualityScore = placeQualityScoreCalculator.calculate(scoredPlace.place());

        double finalScore = scoredPlace.contentScore() * 0.50
                + congestionScore * 0.20
                + transportationScore * 0.15
                + regionTrendScore * 0.10
                + qualityScore * 0.05;

        return scoredPlace.withContextScores(
                congestionScore,
                transportationScore,
                regionTrendScore,
                qualityScore,
                finalScore
        );
    }
}
