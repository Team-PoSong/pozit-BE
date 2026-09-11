package com.pozit.pozitserver.course.service;

import com.pozit.pozitserver.course.domain.TouristSpot;
import com.pozit.pozitserver.course.dto.response.coursespot.TourApiResponse;
import com.pozit.pozitserver.course.repository.TouristSpotRepository;
import com.pozit.pozitserver.global.tourapi.webClient.TourApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TouristSpotSyncService {

    private static final String SUCCESS_CODE = "0000";
    private static final int BATCH_SIZE = 100;

    private final TouristSpotRepository touristSpotRepository;
    private final TourApiClient tourApiClient;
    private final TransactionTemplate transactionTemplate;

    public void syncAll() {
        int pageNumber = 0;
        int totalSynced = 0;
        int totalFailed = 0;
        Page<TouristSpot> page;

        do {
            page = touristSpotRepository.findAll(PageRequest.of(
                    pageNumber,
                    BATCH_SIZE,
                    Sort.by(Sort.Direction.ASC, "id")
            ));

            for (TouristSpot touristSpot : page.getContent()) {
                if (syncOne(touristSpot)) {
                    totalSynced++;
                } else {
                    totalFailed++;
                }
            }

            pageNumber++;
        } while (page.hasNext());

        log.info("Tour API tourist spot sync finished. synced={}, failed={}", totalSynced, totalFailed);
    }

    private boolean syncOne(TouristSpot touristSpot) {
        try {
            TourApiResponse.Response.Item item = fetchDetailCommon(touristSpot);
            updateTouristSpot(touristSpot.getId(), item);
            return true;
        } catch (RuntimeException exception) {
            markSyncFailed(touristSpot.getId());
            log.warn("Tour API tourist spot sync failed. touristSpotId={}, contentId={}",
                    touristSpot.getId(),
                    touristSpot.getContentId(),
                    exception
            );
            return false;
        }
    }

    private TourApiResponse.Response.Item fetchDetailCommon(TouristSpot touristSpot) {
        TourApiResponse response = tourApiClient.getDetailCommon(
                touristSpot.getContentId(),
                touristSpot.getContentTypeId()
        );
        validateResponse(response);

        List<TourApiResponse.Response.Item> items = response.response().body().items() == null
                ? List.of()
                : response.response().body().items().item();
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Tour API detailCommon response has no item.");
        }

        return items.get(0);
    }

    private void validateResponse(TourApiResponse response) {
        if (response == null
                || response.response() == null
                || response.response().header() == null
                || response.response().body() == null) {
            throw new IllegalStateException("Tour API response is invalid.");
        }

        if (!SUCCESS_CODE.equals(response.response().header().resultCode())) {
            throw new IllegalStateException(
                    "Tour API request failed: " + response.response().header().resultMsg()
            );
        }
    }

    private void updateTouristSpot(Long touristSpotId, TourApiResponse.Response.Item item) {
        transactionTemplate.executeWithoutResult(status ->
                touristSpotRepository.findById(touristSpotId)
                        .ifPresent(touristSpot -> touristSpot.updateTourApiInfo(
                                item.contentTypeId(),
                                item.title(),
                                item.legalDongRegionCode(),
                                item.legalDongSigunguCode(),
                                createAddress(item.addr1(), item.addr2()),
                                parseBigDecimal(item.mapy()),
                                parseBigDecimal(item.mapx()),
                                firstNotBlank(item.firstimage(), item.firstimage2())
                        ))
        );
    }

    private void markSyncFailed(Long touristSpotId) {
        transactionTemplate.executeWithoutResult(status ->
                touristSpotRepository.findById(touristSpotId)
                        .ifPresent(TouristSpot::markTourApiSyncFailed)
        );
    }

    private String createAddress(String addr1, String addr2) {
        if (addr1 == null) {
            return null;
        }

        if (addr2 == null || addr2.isBlank()) {
            return addr1;
        }

        return addr1 + " " + addr2;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String firstNotBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
