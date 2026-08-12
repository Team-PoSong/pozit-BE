package com.pozit.pozitserver.recommendation.service;

import org.springframework.stereotype.Component;

@Component
public class CosineSimilarityCalculator {

    public double calculate(double[] userVector, double[] placeVector) {
        double dot = 0.0;
        double userNorm = 0.0;
        double placeNorm = 0.0;

        for (int i = 0; i < userVector.length; i++) {
            dot += userVector[i] * placeVector[i];
            userNorm += userVector[i] * userVector[i];
            placeNorm += placeVector[i] * placeVector[i];
        }

        if (userNorm == 0.0 || placeNorm == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(userNorm) * Math.sqrt(placeNorm));
    }
}
