package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.model.PlaceFeatureVector;
import com.pozit.pozitserver.recommendation.model.RecommendationTag;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContentScoreCalculator {

    private final CosineSimilarityCalculator cosineSimilarityCalculator;

    public ContentScoreCalculator(CosineSimilarityCalculator cosineSimilarityCalculator) {
        this.cosineSimilarityCalculator = cosineSimilarityCalculator;
    }

    public double calculate(double[] userVector, PlaceFeatureVector featureVector, List<RecommendationTag> selectedTags) {
        double cosineSimilarity = cosineSimilarityCalculator.calculate(userVector, featureVector.toArray());
        double tagBalanceScore = selectedTags.stream()
                .mapToDouble(featureVector::scoreOf)
                .min()
                .orElse(0.0);

        return cosineSimilarity * 0.85 + tagBalanceScore * 0.15;
    }
}
