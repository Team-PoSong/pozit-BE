package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import org.springframework.stereotype.Component;

@Component
public class PlaceQualityScoreCalculator {

    public double calculate(CandidatePlace place) {
        double score = 0.0;
        score += place.hasImage() ? 0.20 : 0.0;
        score += place.hasOverview() ? 0.25 : 0.0;
        score += place.hasOperatingInfo() ? 0.20 : 0.0;
        score += place.hasAddress() && place.hasCoordinate() ? 0.25 : 0.0;
        score += place.tel() != null || place.homepage() != null || place.hasParkingInfo() ? 0.10 : 0.0;
        return score;
    }
}
