package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.model.ScoredPlace;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DiversityRerankingService {

    public List<ScoredPlace> rerank(List<ScoredPlace> rankedPlaces) {
        List<ScoredPlace> selected = new ArrayList<>();
        List<ScoredPlace> remaining = new ArrayList<>(rankedPlaces);
        Map<String, Integer> contentTypeCounts = new HashMap<>();
        Map<String, Integer> cat2Counts = new HashMap<>();
        Map<String, Integer> cat3Counts = new HashMap<>();

        while (!remaining.isEmpty()) {
            ScoredPlace next = remaining.stream()
                    .max(Comparator.comparingDouble(place ->
                            rerankScore(place, contentTypeCounts, cat2Counts, cat3Counts)
                    ))
                    .orElseThrow();

            selected.add(next);
            remaining.remove(next);
            increaseCount(contentTypeCounts, next.place().contentTypeId());
            increaseCount(cat2Counts, next.place().cat2());
            increaseCount(cat3Counts, next.place().cat3());
        }

        return selected;
    }

    private double rerankScore(
            ScoredPlace place,
            Map<String, Integer> contentTypeCounts,
            Map<String, Integer> cat2Counts,
            Map<String, Integer> cat3Counts
    ) {
        int sameContentTypeCount = contentTypeCounts.getOrDefault(place.place().contentTypeId(), 0);
        int sameCat2Count = countOf(cat2Counts, place.place().cat2());
        int sameCat3Count = countOf(cat3Counts, place.place().cat3());

        return place.finalScore()
                - sameContentTypeCount * 0.10
                - sameCat2Count * 0.08
                - sameCat3Count * 0.05;
    }

    private void increaseCount(Map<String, Integer> counts, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        counts.merge(key, 1, Integer::sum);
    }

    private int countOf(Map<String, Integer> counts, String key) {
        if (key == null || key.isBlank()) {
            return 0;
        }
        return counts.getOrDefault(key, 0);
    }
}
