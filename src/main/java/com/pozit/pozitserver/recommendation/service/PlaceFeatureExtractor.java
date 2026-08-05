package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import com.pozit.pozitserver.recommendation.model.PlaceFeatureVector;
import com.pozit.pozitserver.recommendation.model.RecommendationTag;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PlaceFeatureExtractor {

    public PlaceFeatureVector extract(CandidatePlace place) {
        double[] vector = new double[RecommendationTag.VECTOR_SIZE];

        applyContentTypeScore(vector, place.contentTypeId());
        applyCategoryScore(vector, place);
        applyKeywordScore(vector, place.searchableText());

        for (int i = 0; i < vector.length; i++) {
            vector[i] = Math.min(1.0, vector[i]);
        }

        return new PlaceFeatureVector(vector);
    }

    private void applyCategoryScore(double[] vector, CandidatePlace place) {
        String categoryText = String.join(" ",
                place.cat1() == null ? "" : place.cat1(),
                place.cat2() == null ? "" : place.cat2(),
                place.cat3() == null ? "" : place.cat3()
        ).toLowerCase(Locale.KOREAN);

        if (categoryText.isBlank()) {
            return;
        }

        if (containsAny(categoryText, "a01", "자연", "생태", "공원")) {
            add(vector, RecommendationTag.HEALING, 0.2);
            add(vector, RecommendationTag.EXPLORATION, 0.1);
        }
        if (containsAny(categoryText, "a02", "역사", "문화")) {
            add(vector, RecommendationTag.CULTURE, 0.2);
            add(vector, RecommendationTag.RECORD, 0.1);
        }
        if (containsAny(categoryText, "a03", "레포츠")) {
            add(vector, RecommendationTag.EXPERIENCE, 0.2);
            add(vector, RecommendationTag.EXPLORATION, 0.2);
        }
        if (containsAny(categoryText, "a04", "쇼핑")) {
            add(vector, RecommendationTag.SHOPPING, 0.2);
        }
        if (containsAny(categoryText, "a05", "음식")) {
            add(vector, RecommendationTag.FOOD, 0.2);
        }
    }

    private void applyContentTypeScore(double[] vector, String contentTypeId) {
        if (contentTypeId == null) {
            add(vector, RecommendationTag.RECORD, 0.2);
            return;
        }

        switch (contentTypeId) {
            case "12" -> {
                add(vector, RecommendationTag.RECORD, 0.6);
                add(vector, RecommendationTag.HEALING, 0.4);
                add(vector, RecommendationTag.CULTURE, 0.5);
                add(vector, RecommendationTag.EXPLORATION, 0.3);
            }
            case "14" -> {
                add(vector, RecommendationTag.CULTURE, 0.8);
                add(vector, RecommendationTag.ART, 0.8);
                add(vector, RecommendationTag.RECORD, 0.4);
            }
            case "15" -> {
                add(vector, RecommendationTag.EXPERIENCE, 0.7);
                add(vector, RecommendationTag.CULTURE, 0.5);
                add(vector, RecommendationTag.ART, 0.4);
            }
            case "28" -> {
                add(vector, RecommendationTag.EXPERIENCE, 0.8);
                add(vector, RecommendationTag.EXPLORATION, 0.8);
                add(vector, RecommendationTag.HEALING, 0.3);
            }
            case "38" -> {
                add(vector, RecommendationTag.SHOPPING, 0.9);
                add(vector, RecommendationTag.FOOD, 0.4);
                add(vector, RecommendationTag.CULTURE, 0.3);
            }
            case "39" -> {
                add(vector, RecommendationTag.FOOD, 1.0);
                add(vector, RecommendationTag.EXPERIENCE, 0.2);
            }
            default -> add(vector, RecommendationTag.RECORD, 0.2);
        }
    }

    private void applyKeywordScore(double[] vector, String text) {
        if (text == null) {
            return;
        }

        String normalizedTitle = text.toLowerCase(Locale.KOREAN);

        if (containsAny(normalizedTitle, "미술관", "갤러리", "전시", "공연", "극장")) {
            add(vector, RecommendationTag.ART, 0.2);
        }
        if (containsAny(normalizedTitle, "시장", "먹거리", "맛집", "식당", "카페", "거리")) {
            add(vector, RecommendationTag.FOOD, 0.2);
        }
        if (containsAny(normalizedTitle, "공원", "숲", "수목원", "휴양림", "정원")) {
            add(vector, RecommendationTag.HEALING, 0.2);
        }
        if (containsAny(normalizedTitle, "산", "동굴", "섬", "트레킹", "둘레길", "해변")) {
            add(vector, RecommendationTag.EXPLORATION, 0.2);
        }
        if (containsAny(normalizedTitle, "체험", "공방", "만들기", "레포츠")) {
            add(vector, RecommendationTag.EXPERIENCE, 0.2);
        }
        if (containsAny(normalizedTitle, "대표메뉴", "메뉴", "한식", "분식", "디저트", "커피")) {
            add(vector, RecommendationTag.FOOD, 0.2);
        }
        if (containsAny(normalizedTitle, "궁", "유적", "역사", "박물관", "문화")) {
            add(vector, RecommendationTag.CULTURE, 0.2);
            add(vector, RecommendationTag.RECORD, 0.1);
        }
        if (containsAny(normalizedTitle, "쇼핑", "몰", "상가", "백화점")) {
            add(vector, RecommendationTag.SHOPPING, 0.2);
        }
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void add(double[] vector, RecommendationTag tag, double score) {
        vector[tag.index()] += score;
    }
}
