package com.pozit.pozitserver.recommendation.model;

public record ScoredPlace(
        CandidatePlace place,
        PlaceFeatureVector featureVector,
        double contentScore,
        double congestionScore,
        double transportationScore,
        double regionTrendScore,
        double qualityScore,
        double finalScore
) {

    public static ScoredPlace contentOnly(
            CandidatePlace place,
            PlaceFeatureVector featureVector,
            double contentScore
    ) {
        return new ScoredPlace(place, featureVector, contentScore, 0.5, 0.5, 0.5, 0.5, contentScore);
    }

    public ScoredPlace withContextScores(
            double congestionScore,
            double transportationScore,
            double regionTrendScore,
            double qualityScore,
            double finalScore
    ) {
        return new ScoredPlace(
                place,
                featureVector,
                contentScore,
                congestionScore,
                transportationScore,
                regionTrendScore,
                qualityScore,
                finalScore
        );
    }
}
