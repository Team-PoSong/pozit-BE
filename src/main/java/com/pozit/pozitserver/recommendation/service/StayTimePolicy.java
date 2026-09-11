package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.travel.domain.TravelStyle;
import org.springframework.stereotype.Component;

@Component
public class StayTimePolicy {

    public int stayMinutes(String contentTypeId) {
        return stayMinutes(contentTypeId, TravelStyle.NORMAL);
    }

    public int stayMinutes(String contentTypeId, TravelStyle travelStyle) {
        return Math.max(30, (int) Math.round(baseStayMinutes(contentTypeId) * styleMultiplier(travelStyle)));
    }

    private int baseStayMinutes(String contentTypeId) {
        if (contentTypeId == null) {
            return 75;
        }

        return switch (contentTypeId) {
            case "14" -> 120;
            case "15" -> 120;
            case "28" -> 120;
            case "38" -> 90;
            case "39" -> 75;
            default -> 90;
        };
    }

    private double styleMultiplier(TravelStyle travelStyle) {
        if (travelStyle == null) {
            return 1.0;
        }

        return switch (travelStyle) {
            case RELAXED -> 1.2;
            case NORMAL -> 1.0;
            case TIGHT -> 0.8;
        };
    }
}
