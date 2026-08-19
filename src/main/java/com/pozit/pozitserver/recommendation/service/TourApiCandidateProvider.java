package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.course.dto.response.coursespot.TourApiResponse;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.global.tourapi.webClient.TourApiClient;
import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import com.pozit.pozitserver.recommendation.model.CourseRecommendCommand;
import com.pozit.pozitserver.recommendation.model.RecommendationTag;
import com.pozit.pozitserver.recommendation.model.TourApiRegionCodes;
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
    private final TourApiRegionCodeResolver regionCodeResolver;

    public List<CandidatePlace> findCandidates(CourseRecommendCommand command) {
        TourApiRegionCodes regionCodes = regionCodeResolver.resolve(command.regionCode());
        List<Map<String, CandidatePlace>> candidateBuckets = new ArrayList<>();

        Map<String, CandidatePlace> destinationCandidates = collectKeywordCandidates(command.destination(), regionCodes);
        if (!destinationCandidates.isEmpty()) {
            candidateBuckets.add(destinationCandidates);
        }

        for (RecommendationTag tag : command.tags()) {
            Map<String, CandidatePlace> tagCandidates = collectTagCandidates(tag, command, regionCodes);
            if (!tagCandidates.isEmpty()) {
                candidateBuckets.add(tagCandidates);
            }
        }

        if (candidateBuckets.isEmpty()) {
            Map<String, CandidatePlace> fallbackCandidates = collectContentTypeCandidates(
                    List.of("12", "14", "39"),
                    regionCodes
            );
            if (!fallbackCandidates.isEmpty()) {
                candidateBuckets.add(fallbackCandidates);
            }
        }

        return selectBalancedCandidates(candidateBuckets, MAX_ENRICH_TARGETS).stream()
                .map(this::enrich)
                .filter(place -> !isFinishedEvent(place, command))
                .toList();
    }

    public List<CandidatePlace> findKeywordCandidates(
            CourseRecommendCommand command,
            List<String> keywords,
            int maxCount
    ) {
        TourApiRegionCodes regionCodes = regionCodeResolver.resolve(command.regionCode());
        Map<String, CandidatePlace> candidatesByContentId = new LinkedHashMap<>();

        for (String keyword : keywords) {
            if (candidatesByContentId.size() >= maxCount) {
                break;
            }

            collectKeywordCandidates(keyword, regionCodes).values().stream()
                    .limit(Math.max(0, maxCount - candidatesByContentId.size()))
                    .forEach(place -> candidatesByContentId.putIfAbsent(place.contentId(), place));
        }

        return candidatesByContentId.values().stream()
                .limit(maxCount)
                .map(this::enrich)
                .filter(place -> !isFinishedEvent(place, command))
                .toList();
    }

    private Map<String, CandidatePlace> collectTagCandidates(
            RecommendationTag tag,
            CourseRecommendCommand command,
            TourApiRegionCodes regionCodes
    ) {
        Map<String, CandidatePlace> candidatesByContentId = new LinkedHashMap<>();
        candidatesByContentId.putAll(collectContentTypeCandidates(buildContentTypeIds(tag), regionCodes));
        candidatesByContentId.putAll(collectKeywordCandidates(buildKeyword(command.destination(), tag), regionCodes));
        return candidatesByContentId;
    }

    private Map<String, CandidatePlace> collectContentTypeCandidates(
            List<String> contentTypeIds,
            TourApiRegionCodes regionCodes
    ) {
        Map<String, CandidatePlace> candidatesByContentId = new LinkedHashMap<>();

        for (String contentTypeId : contentTypeIds) {
            TourApiResponse response = tourApiClient.findAreaBasedPlaces(
                    regionCodes.legalDongRegionCode(),
                    regionCodes.legalDongSigunguCode(),
                    regionCodes.areaCode(),
                    regionCodes.sigunguCode(),
                    contentTypeId,
                    PAGE,
                    SIZE_PER_QUERY
            );
            validateResponse(response);

            extractItems(response).stream()
                    .map(CandidatePlace::from)
                    .filter(this::isUsable)
                    .filter(place -> regionCodeResolver.matches(place, regionCodes))
                    .forEach(place -> candidatesByContentId.putIfAbsent(place.contentId(), place));
        }

        return candidatesByContentId;
    }

    private Map<String, CandidatePlace> collectKeywordCandidates(
            String keyword,
            TourApiRegionCodes regionCodes
    ) {
        Map<String, CandidatePlace> candidatesByContentId = new LinkedHashMap<>();

        if (keyword == null || keyword.isBlank()) {
            return candidatesByContentId;
        }

        TourApiResponse response = tourApiClient.searchPlaces(keyword, PAGE, SIZE_PER_QUERY);
        validateResponse(response);

        extractItems(response).stream()
                .map(CandidatePlace::from)
                .filter(this::isUsable)
                .filter(place -> regionCodeResolver.matches(place, regionCodes))
                .forEach(place -> candidatesByContentId.putIfAbsent(place.contentId(), place));

        return candidatesByContentId;
    }

    private List<CandidatePlace> selectBalancedCandidates(
            List<Map<String, CandidatePlace>> candidateBuckets,
            int maxCount
    ) {
        Map<String, CandidatePlace> selectedCandidates = new LinkedHashMap<>();
        List<List<CandidatePlace>> buckets = candidateBuckets.stream()
                .<List<CandidatePlace>>map(bucket -> new ArrayList<>(bucket.values()))
                .toList();
        int index = 0;
        boolean hasCandidateAtIndex;

        do {
            hasCandidateAtIndex = false;

            for (List<CandidatePlace> bucket : buckets) {
                if (index >= bucket.size()) {
                    continue;
                }

                hasCandidateAtIndex = true;
                CandidatePlace candidate = bucket.get(index);
                if (!selectedCandidates.containsKey(candidate.contentId())) {
                    selectedCandidates.put(candidate.contentId(), candidate);
                }

                if (selectedCandidates.size() >= maxCount) {
                    return new ArrayList<>(selectedCandidates.values());
                }
            }

            index++;
        } while (hasCandidateAtIndex);

        return new ArrayList<>(selectedCandidates.values());
    }

    private List<String> buildContentTypeIds(RecommendationTag tag) {
        return switch (tag) {
            case RECORD -> List.of("12", "14");
            case FOOD -> List.of("39", "38");
            case HEALING -> List.of("12");
            case EXPERIENCE -> List.of("15", "28", "12");
            case CULTURE, ART -> List.of("14", "12", "15");
            case SHOPPING -> List.of("38");
            case EXPLORATION -> List.of("12", "28");
        };
    }

    private String buildKeyword(String destination, RecommendationTag tag) {
        return switch (tag) {
            case RECORD -> destination + " 관광지";
            case FOOD -> destination + " 맛집";
            case HEALING -> destination + " 공원";
            case EXPERIENCE -> destination + " 체험";
            case CULTURE -> destination + " 문화";
            case ART -> destination + " 미술관";
            case SHOPPING -> destination + " 시장";
            case EXPLORATION -> destination + " 트레킹";
        };
    }

    private CandidatePlace enrich(CandidatePlace place) {
        CandidatePlace enriched = place;
        enriched = mergeFirstItem(enriched, () -> tourApiClient.getDetailCommon(place.contentId(), place.contentTypeId()));
        enriched = mergeFirstItem(enriched, () -> tourApiClient.getDetailIntro(place.contentId(), place.contentTypeId()));
        enriched = mergeRepeatedInfoItems(enriched, () -> tourApiClient.getDetailInfo(place.contentId(), place.contentTypeId()));
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

    private CandidatePlace mergeRepeatedInfoItems(CandidatePlace base, java.util.function.Supplier<TourApiResponse> supplier) {
        try {
            TourApiResponse response = supplier.get();
            validateResponse(response);
            List<CandidatePlace> repeatedInfoPlaces = extractItems(response).stream()
                    .map(CandidatePlace::from)
                    .toList();
            return base.mergeRepeatedInfos(repeatedInfoPlaces);
        } catch (RuntimeException exception) {
            log.debug("Tour API repeated info enrichment failed. contentId={}", base.contentId(), exception);
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
