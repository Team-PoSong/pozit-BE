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
    private static final int SIZE_PER_QUERY = 20;
    private static final int MAX_ENRICH_TARGETS = 40;

    private final TourApiClient tourApiClient;

    public List<CandidatePlace> findCandidates(CourseRecommendCommand command) {
        Map<String, CandidatePlace> candidatesByContentId = new LinkedHashMap<>();
        String legalDongRegionCode = toLegalDongRegionCode(command.regionCode());
        String legalDongSigunguCode = toLegalDongSigunguCode(command.regionCode());

        for (String contentTypeId : buildPriorityContentTypeIds(command.tags())) {
            TourApiResponse response = tourApiClient.findAreaBasedPlaces(
                    legalDongRegionCode,
                    legalDongSigunguCode,
                    contentTypeId,
                    PAGE,
                    SIZE_PER_QUERY
            );
            validateResponse(response);

            extractItems(response).stream()
                    .map(CandidatePlace::from)
                    .filter(this::isUsable)
                    .filter(place -> isInRegion(place, command.regionCode()))
                    .forEach(place -> candidatesByContentId.putIfAbsent(place.contentId(), place));
        }

        for (String keyword : buildKeywords(command)) {
            TourApiResponse response = tourApiClient.searchPlaces(keyword, PAGE, SIZE_PER_QUERY);
            validateResponse(response);

            extractItems(response).stream()
                    .map(CandidatePlace::from)
                    .filter(this::isUsable)
                    .filter(place -> isInRegion(place, command.regionCode()))
                    .forEach(place -> candidatesByContentId.putIfAbsent(place.contentId(), place));
        }

        return new ArrayList<>(candidatesByContentId.values()).stream()
                .limit(MAX_ENRICH_TARGETS)
                .map(this::enrich)
                .filter(place -> !isFinishedEvent(place, command))
                .toList();
    }

    private List<String> buildPriorityContentTypeIds(List<RecommendationTag> tags) {
        List<String> contentTypeIds = new ArrayList<>();

        for (RecommendationTag tag : tags) {
            switch (tag) {
                case RECORD -> {
                    contentTypeIds.add("12");
                    contentTypeIds.add("14");
                }
                case FOOD -> {
                    contentTypeIds.add("39");
                    contentTypeIds.add("38");
                }
                case HEALING -> contentTypeIds.add("12");
                case EXPERIENCE -> {
                    contentTypeIds.add("15");
                    contentTypeIds.add("28");
                    contentTypeIds.add("12");
                }
                case CULTURE, ART -> {
                    contentTypeIds.add("14");
                    contentTypeIds.add("12");
                    contentTypeIds.add("15");
                }
                case SHOPPING -> contentTypeIds.add("38");
                case EXPLORATION -> {
                    contentTypeIds.add("12");
                    contentTypeIds.add("28");
                }
            }
        }

        if (contentTypeIds.isEmpty()) {
            contentTypeIds.addAll(List.of("12", "14", "39"));
        }

        return contentTypeIds.stream()
                .distinct()
                .toList();
    }

    private CandidatePlace enrich(CandidatePlace place) {
        CandidatePlace enriched = place;
        enriched = mergeFirstItem(enriched, () -> tourApiClient.getDetailCommon(place.contentId(), place.contentTypeId()));
        enriched = mergeFirstItem(enriched, () -> tourApiClient.getDetailIntro(place.contentId(), place.contentTypeId()));
        enriched = mergeFirstItem(enriched, () -> tourApiClient.getDetailInfo(place.contentId(), place.contentTypeId()));
        return enriched;
    }

    private CandidatePlace mergeFirstItem(CandidatePlace base, java.util.function.Supplier<TourApiResponse> supplier) {
        try {
            TourApiResponse response = supplier.get();
            validateResponse(response);
            return extractItems(response).stream()
                    .findFirst()
                    .map(CandidatePlace::from)
                    .map(base::merge)
                    .orElse(base);
        } catch (RuntimeException exception) {
            log.debug("Tour API detail enrichment failed. contentId={}", base.contentId(), exception);
            return base;
        }
    }

    private boolean isFinishedEvent(CandidatePlace place, CourseRecommendCommand command) {
        if (!"15".equals(place.contentTypeId()) || place.eventEndDate() == null) {
            return false;
        }

        try {
            java.time.LocalDate eventEndDate = java.time.LocalDate.parse(
                    place.eventEndDate(),
                    java.time.format.DateTimeFormatter.BASIC_ISO_DATE
            );
            return eventEndDate.isBefore(command.startDate());
        } catch (RuntimeException exception) {
            return false;
        }
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
