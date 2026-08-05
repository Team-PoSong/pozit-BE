package com.pozit.pozitserver.recommendation.model;

import java.util.Arrays;
import java.util.Optional;

public enum RecommendationTag {

    RECORD("기록", 0),
    FOOD("미식", 1),
    HEALING("힐링", 2),
    EXPERIENCE("체험", 3),
    CULTURE("문화", 4),
    ART("예술", 5),
    SHOPPING("쇼핑", 6),
    EXPLORATION("탐험", 7);

    public static final int VECTOR_SIZE = 8;

    private final String koreanName;
    private final int index;

    RecommendationTag(String koreanName, int index) {
        this.koreanName = koreanName;
        this.index = index;
    }

    public String koreanName() {
        return koreanName;
    }

    public int index() {
        return index;
    }

    public static Optional<RecommendationTag> fromName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        String normalizedName = name.trim();
        return Arrays.stream(values())
                .filter(tag -> tag.name().equalsIgnoreCase(normalizedName)
                        || tag.koreanName.equals(normalizedName))
                .findFirst();
    }
}
