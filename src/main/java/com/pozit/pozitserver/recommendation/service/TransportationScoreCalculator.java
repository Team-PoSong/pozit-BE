package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import com.pozit.pozitserver.travel.domain.Transportation;
import org.springframework.stereotype.Component;

@Component
public class TransportationScoreCalculator {

    public double calculate(CandidatePlace place, Transportation transportation) {
        if (transportation == null) {
            return 0.5;
        }

        return switch (transportation) {
            case WALK -> walkingScore(place);
            case CAR -> carScore(place);
            case PUBLIC -> publicTransitScore(place);
        };
    }

    private double walkingScore(CandidatePlace place) {
        if (place.contentTypeId() != null && place.contentTypeId().equals("28")) {
            return 0.8;
        }
        return place.hasCoordinate() ? 0.7 : 0.4;
    }

    private double carScore(CandidatePlace place) {
        if (place.contentTypeId() != null && place.contentTypeId().equals("39")) {
            return 0.6;
        }
        return place.hasAddress() ? 0.7 : 0.5;
    }

    private double publicTransitScore(CandidatePlace place) {
        return place.hasAddress() && place.hasCoordinate() ? 0.7 : 0.5;
    }
}
