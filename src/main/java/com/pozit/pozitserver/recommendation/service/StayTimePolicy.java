package com.pozit.pozitserver.recommendation.service;

import org.springframework.stereotype.Component;

@Component
public class StayTimePolicy {

    public int stayMinutes(String contentTypeId) {
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
}
