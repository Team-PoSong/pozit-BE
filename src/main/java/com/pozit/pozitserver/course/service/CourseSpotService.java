package com.pozit.pozitserver.course.service;

import com.pozit.pozitserver.course.dto.response.coursespot.PlaceSearchItemResponse;
import com.pozit.pozitserver.course.dto.response.coursespot.PlaceSearchResponse;
import com.pozit.pozitserver.course.dto.response.coursespot.TourApiResponse;
import com.pozit.pozitserver.global.tourapi.webClient.TourApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CourseSpotService {

    private static final String SUCCESS_CODE = "0000";

    private final TourApiClient tourApiClient;

    public PlaceSearchResponse search(
            String keyword,
            int page,
            int size
    ) {
        String normalizedKeyword = keyword.trim();

        TourApiResponse result =
                tourApiClient.searchPlaces(normalizedKeyword, page, size);

        validateResponse(result);

        TourApiResponse.Response.Body body = result.response().body();

        List<TourApiResponse.Response.Item> items =
                body.items() == null || body.items().item() == null
                        ? List.of()
                        : body.items().item();

        List<PlaceSearchItemResponse> places = items.stream()
                .map(PlaceSearchItemResponse::from)
                .toList();

        return new PlaceSearchResponse(
                body.pageNo(),
                body.numOfRows(),
                body.totalCount(),
                places
        );
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
}
