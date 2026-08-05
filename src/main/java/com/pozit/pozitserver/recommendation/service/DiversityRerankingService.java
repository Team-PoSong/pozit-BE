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

        while (!remaining.isEmpty()) {
            ScoredPlace next = remaining.stream()
                    .max(Comparator.comparingDouble(place ->
                            rerankScore(place, contentTypeCounts)
                    ))
                    .orElseThrow();

            selected.add(next);
            remaining.remove(next);
            contentTypeCounts.merge(next.place().contentTypeId(), 1, Integer::sum);
        }

        return selected;
    }

    private double rerankScore(ScoredPlace place, Map<String, Integer> contentTypeCounts) {
        int sameContentTypeCount = contentTypeCounts.getOrDefault(place.place().contentTypeId(), 0);
        return place.finalScore() - sameContentTypeCount * 0.10;
    }
}
