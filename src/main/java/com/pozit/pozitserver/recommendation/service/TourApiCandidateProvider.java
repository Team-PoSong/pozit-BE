package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.course.dto.response.coursespot.TourApiResponse;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.global.tourapi.webClient.TourApiClient;
import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import com.pozit.pozitserver.recommendation.model.CourseRecommendCommand;
import com.pozit.pozitserver.recommendation.model.RecommendationTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TourApiCandidateProvider {

    private static final String SUCCESS_CODE = "0000";
    private static final int PAGE = 1;
    private static final int SIZE_PER_QUERY = 15;

    private final TourApiClient tourApiClient;

    public List<CandidatePlace> findCandidates(CourseRecommendCommand command) {
        Map<String, CandidatePlace> candidatesByContentId = new LinkedHashMap<>();

        for (String keyword : buildKeywords(command)) {
            TourApiResponse response = tourApiClient.searchPlaces(keyword, PAGE, SIZE_PER_QUERY);
            validateResponse(response);

            extractItems(response).stream()
                    .map(CandidatePlace::from)
                    .filter(this::isUsable)
                    .filter(place -> isInRegion(place, command.regionCode()))
                    .forEach(place -> candidatesByContentId.putIfAbsent(place.contentId(), place));
        }

        return new ArrayList<>(candidatesByContentId.values());
    }

    private List<String> buildKeywords(CourseRecommendCommand command) {
        List<String> keywords = new ArrayList<>();
        keywords.add(command.destination());

        for (RecommendationTag tag : command.tags()) {
            switch (tag) {
                case RECORD -> keywords.add(command.destination() + " 관광지");
                case FOOD -> keywords.add(command.destination() + " 맛집");
                case HEALING -> keywords.add(command.destination() + " 공원");
                case EXPERIENCE -> keywords.add(command.destination() + " 체험");
                case CULTURE -> keywords.add(command.destination() + " 문화");
                case ART -> keywords.add(command.destination() + " 미술관");
                case SHOPPING -> keywords.add(command.destination() + " 시장");
                case EXPLORATION -> keywords.add(command.destination() + " 트레킹");
            }
        }

        return keywords.stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .distinct()
                .toList();
    }

    private List<TourApiResponse.Response.Item> extractItems(TourApiResponse response) {
        TourApiResponse.Response.Body body = response.response().body();
        if (body == null || body.items() == null || body.items().item() == null) {
            return List.of();
        }
        return body.items().item();
    }

    private boolean isUsable(CandidatePlace place) {
        return place.contentId() != null
                && place.title() != null
                && place.hasCoordinate();
    }

    private boolean isInRegion(CandidatePlace place, String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return true;
        }

        String legalDongRegionCode = toLegalDongRegionCode(regionCode);
        String legalDongSigunguCode = toLegalDongSigunguCode(regionCode);

        if (!legalDongSigunguCode.isBlank()) {
            return legalDongSigunguCode.equals(place.legalDongSigunguCode());
        }

        if (!legalDongRegionCode.isBlank()) {
            return legalDongRegionCode.equals(place.legalDongRegionCode());
        }

        return true;
    }

    private String toLegalDongRegionCode(String regionCode) {
        if (regionCode.length() < 2) {
            return "";
        }
        return regionCode.substring(0, 2);
    }

    private String toLegalDongSigunguCode(String regionCode) {
        if (regionCode.length() < 5 || regionCode.endsWith("000")) {
            return "";
        }
        return regionCode;
    }

    private void validateResponse(TourApiResponse result) {
        if (result == null
                || result.response() == null
                || result.response().header() == null) {
            throw new BusinessException(ErrorCode.TOUR_API_REQUEST_FAILED);
        }

        if (!SUCCESS_CODE.equals(result.response().header().resultCode())) {
            log.warn("Tour API recommendation request failed: {}", result.response().header().resultMsg());
            throw new BusinessException(ErrorCode.TOUR_API_REQUEST_FAILED);
        }
    }
}
