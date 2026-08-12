package com.pozit.pozitserver.recommendation.model;

import java.util.Arrays;

public record PlaceFeatureVector(double[] values) {

    public PlaceFeatureVector {
        if (values.length != RecommendationTag.VECTOR_SIZE) {
            throw new IllegalArgumentException("추천 태그 벡터는 8차원이어야 합니다.");
        }
        values = Arrays.copyOf(values, values.length);
    }

    public double scoreOf(RecommendationTag tag) {
        return values[tag.index()];
    }

    public double[] toArray() {
        return Arrays.copyOf(values, values.length);
    }
}
