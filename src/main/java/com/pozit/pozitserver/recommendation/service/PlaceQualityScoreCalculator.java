package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import org.springframework.stereotype.Component;

@Component
public class PlaceQualityScoreCalculator {

    public double calculate(CandidatePlace place) {
        double score = 0.0;
        score += place.hasImage() ? 0.20 : 0.0;
        score += place.title() != null ? 0.20 : 0.0;
        score += place.hasAddress() ? 0.25 : 0.0;
        score += place.hasCoordinate() ? 0.25 : 0.0;
        score += place.tel() != null ? 0.10 : 0.0;
        return score;
    }
}
