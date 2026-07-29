package com.pozit.pozitserver.course.service;

import com.pozit.pozitserver.course.domain.TouristSpot;
import com.pozit.pozitserver.course.dto.request.CourseSpotRequest;
import com.pozit.pozitserver.course.dto.response.coursespot.CourseSpotSaveResponse;
import com.pozit.pozitserver.course.dto.response.coursespot.HostTouristSpotRankResponse;
import com.pozit.pozitserver.course.dto.response.coursespot.HostTouristSpotRankScrollResponse;
import com.pozit.pozitserver.course.dto.response.coursespot.PlaceSearchItemResponse;
import com.pozit.pozitserver.course.dto.response.coursespot.PlaceSearchResponse;
import com.pozit.pozitserver.course.dto.response.coursespot.TourApiResponse;
import com.pozit.pozitserver.course.repository.TouristSpotRepository;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.global.tourapi.webClient.TourApiClient;
import com.pozit.pozitserver.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TouristSpotService {

    private static final String SUCCESS_CODE = "0000";
    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(30);

    private final TourApiClient tourApiClient;
    private final TouristSpotRepository touristSpotRepository;
    private final Map<String, CachedTouristSpot> searchCache = new ConcurrentHashMap<>();

    public HostTouristSpotRankScrollResponse getHostTouristSpotsRank(
            String regionCode,
            int cursor,
            int size
    ) {
        String normalizedRegionCode = regionCode == null ? "" : regionCode.trim();
        String legalDongRegionCode = toLegalDongRegionCode(normalizedRegionCode);
        String legalDongSigunguCode = toLegalDongSigunguCode(normalizedRegionCode);

        Pageable pageable = PageRequest.of(cursor - 1, size + 1);
        List<TouristSpotRepository.TouristSpotRankProjection> ranks =
                touristSpotRepository.findHostTouristSpotsRank(
                        normalizedRegionCode,
                        legalDongRegionCode,
                        legalDongSigunguCode,
                        pageable
                );

        boolean hasNext = ranks.size() > size;
        List<TouristSpotRepository.TouristSpotRankProjection> currentRanks = hasNext
                ? ranks.subList(0, size)
                : ranks;

        List<HostTouristSpotRankResponse> responses = new ArrayList<>();
        int rankOffset = (cursor - 1) * size;

        for (int i = 0; i < currentRanks.size(); i++) {
            TouristSpotRepository.TouristSpotRankProjection rank = currentRanks.get(i);
            responses.add(new HostTouristSpotRankResponse(
                    rankOffset + i + 1,
                    rank.getTouristSpotId(),
                    rank.getTitle(),
                    rank.getAddress(),
                    rank.getImageUrl(),
                    rank.getCourseSpotCount()
            ));
        }

        return new HostTouristSpotRankScrollResponse(
                cursor,
                hasNext ? cursor + 1 : null,
                hasNext,
                responses.size(),
                responses
        );
    }

    private String toLegalDongRegionCode(String regionCode) {
        if (regionCode == null || regionCode.length() < 2) {
            return "";
        }

        return regionCode.substring(0, 2);
    }

    private String toLegalDongSigunguCode(String regionCode) {
        if (regionCode == null || regionCode.length() < 5 || regionCode.endsWith("000")) {
            return "";
        }

        return regionCode;
    }

    public PlaceSearchResponse search(
            String keyword,
            int cursor,
            int size
    ) {
        String normalizedKeyword = keyword.trim();

        TourApiResponse result =
                tourApiClient.searchPlaces(normalizedKeyword, cursor, size);

        validateResponse(result);

        TourApiResponse.Response.Body body = result.response().body();

        List<TourApiResponse.Response.Item> items =
                body.items() == null || body.items().item() == null
                        ? List.of()
                        : body.items().item();

        cacheSearchItems(items);

        List<PlaceSearchItemResponse> places = items.stream()
                .map(PlaceSearchItemResponse::from)
                .toList();

        boolean hasNext = body.pageNo() * body.numOfRows() < body.totalCount();
        Integer nextCursor = hasNext ? body.pageNo() + 1 : null;

        return new PlaceSearchResponse(
                body.pageNo(),
                nextCursor,
                hasNext,
                places.size(),
                places
        );
    }

    @Transactional
    public CourseSpotSaveResponse saveSpotsToCourse(
            CourseSpotRequest request
    ) {
        List<String> contentIds = request.contentIds().stream()
                .map(String::trim)
                .filter(contentId -> !contentId.isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));

        Map<String, TouristSpot> touristSpotMap =
                touristSpotRepository.findByContentIdIn(contentIds).stream()
                        .collect(Collectors.toMap(
                                TouristSpot::getContentId,
                                Function.identity()
                        ));

        List<TouristSpot> spotsToSave = new ArrayList<>();

        for (String contentId : contentIds) {
            if (touristSpotMap.containsKey(contentId)) {
                continue;
            }

            CachedTouristSpot cachedSpot = getCachedSpot(contentId);
            spotsToSave.add(cachedSpot.toEntity());
        }

        touristSpotRepository.saveAll(spotsToSave)
                .forEach(touristSpot ->
                        touristSpotMap.put(touristSpot.getContentId(), touristSpot)
                );

        List<CourseSpotSaveResponse.SavedSpot> savedSpots = contentIds.stream()
                .map(contentId -> {
                    TouristSpot touristSpot = touristSpotMap.get(contentId);

                    return new CourseSpotSaveResponse.SavedSpot(
                            contentId,
                            touristSpot.getId(),
                            touristSpot.getName(),
                            touristSpot.getAddress()
                    );
                })
                .toList();

        return new CourseSpotSaveResponse(savedSpots);
    }

    private void validateResponse(TourApiResponse result) {
        if (result == null
                || result.response() == null
                || result.response().header() == null) {
            throw new IllegalStateException(
                    "관광공사 API 응답을 확인할 수 없습니다."
            );
        }

        TourApiResponse.Response.Header header = result.response().header();

        if (!SUCCESS_CODE.equals(header.resultCode())) {
            throw new IllegalStateException(
                    "관광공사 API 요청에 실패했습니다: "
                            + header.resultMsg()
            );
        }
    }

    private void cacheSearchItems(List<TourApiResponse.Response.Item> items) {
        Instant expiresAt = Instant.now().plus(SEARCH_CACHE_TTL);
        evictExpiredSearchCache();

        items.stream()
                .filter(item -> item.contentId() != null && !item.contentId().isBlank())
                .map(item -> CachedTouristSpot.from(item, expiresAt))
                .forEach(cachedSpot ->
                        searchCache.put(cachedSpot.contentId(), cachedSpot)
                );
    }

    private CachedTouristSpot getCachedSpot(String contentId) {
        CachedTouristSpot cachedSpot = searchCache.get(contentId);

        if (cachedSpot == null || cachedSpot.isExpired()) {
            searchCache.remove(contentId);
            throw new BusinessException(ErrorCode.SEARCHED_TOURIST_SPOT_NOT_FOUND);
        }

        return cachedSpot;
    }

    private void evictExpiredSearchCache() {
        Instant now = Instant.now();
        searchCache.entrySet()
                .removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }
}
