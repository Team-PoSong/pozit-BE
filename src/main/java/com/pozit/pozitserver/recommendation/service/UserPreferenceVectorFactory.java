package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.model.RecommendationTag;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserPreferenceVectorFactory {

    public double[] create(List<RecommendationTag> tags) {
        double[] vector = new double[RecommendationTag.VECTOR_SIZE];

        for (RecommendationTag tag : tags) {
            vector[tag.index()] = 1.0;
        }

        return vector;
    }
}
