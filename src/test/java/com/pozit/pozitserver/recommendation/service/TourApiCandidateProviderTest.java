package com.pozit.pozitserver.recommendation.service;

import com.pozit.pozitserver.course.dto.response.coursespot.TourApiResponse;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.global.tourapi.webClient.TourApiClient;
import com.pozit.pozitserver.recommendation.model.CandidatePlace;
import com.pozit.pozitserver.recommendation.model.CourseRecommendCommand;
import com.pozit.pozitserver.recommendation.model.RecommendationTag;
import com.pozit.pozitserver.recommendation.model.TourApiRegionCodes;
import com.pozit.pozitserver.travel.domain.Transportation;
import com.pozit.pozitserver.travel.domain.TravelStyle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TourApiCandidateProviderTest {

    private final TourApiClient tourApiClient = mock(TourApiClient.class);
    private final TourApiRegionCodeResolver regionCodeResolver = mock(TourApiRegionCodeResolver.class);
    private final TourApiCandidateProvider candidateProvider = new TourApiCandidateProvider(
            tourApiClient,
            regionCodeResolver
    );

    @Test
    void findCandidatesFallsBackWhenKeywordCandidateCollectionFails() {
        TourApiRegionCodes regionCodes = new TourApiRegionCodes("", "", "", "");
        when(regionCodeResolver.resolve("11")).thenReturn(regionCodes);
        when(regionCodeResolver.matches(any(CandidatePlace.class), eq(regionCodes))).thenReturn(true);
        when(tourApiClient.searchPlaces(eq("서울"), anyInt(), anyInt()))
                .thenThrow(new BusinessException(ErrorCode.TOUR_API_REQUEST_FAILED));
        when(tourApiClient.findAreaBasedPlaces(anyString(), anyString(), anyString(), anyString(), eq("12"), anyInt(), anyInt()))
                .thenReturn(successResponse(item("100", "12", "Fallback Place")));
        when(tourApiClient.findAreaBasedPlaces(anyString(), anyString(), anyString(), anyString(), eq("14"), anyInt(), anyInt()))
                .thenReturn(successResponse());
        when(tourApiClient.findAreaBasedPlaces(anyString(), anyString(), anyString(), anyString(), eq("39"), anyInt(), anyInt()))
                .thenReturn(successResponse());

        List<CandidatePlace> candidates = candidateProvider.findCandidates(command());

        assertThat(candidates)
                .extracting(CandidatePlace::contentId)
                .containsExactly("100");
    }

    @Test
    void findCandidatesFallsBackWhenCollectedCandidatesAreRemovedByFinalFilter() {
        TourApiRegionCodes regionCodes = new TourApiRegionCodes("", "", "", "");
        CourseRecommendCommand command = new CourseRecommendCommand(
                1L,
                "서울",
                "11",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                TravelStyle.NORMAL,
                Transportation.PUBLIC,
                List.of(RecommendationTag.EXPERIENCE)
        );
        when(regionCodeResolver.resolve("11")).thenReturn(regionCodes);
        when(regionCodeResolver.matches(any(CandidatePlace.class), eq(regionCodes))).thenReturn(true);
        when(tourApiClient.searchPlaces(eq("서울"), anyInt(), anyInt()))
                .thenReturn(successResponse());
        when(tourApiClient.searchPlaces(eq("서울 체험"), anyInt(), anyInt()))
                .thenReturn(successResponse());
        when(tourApiClient.findAreaBasedPlaces(anyString(), anyString(), anyString(), anyString(), eq("15"), anyInt(), anyInt()))
                .thenReturn(successResponse(eventItem("200", "15", "Finished Festival", "20250831")));
        when(tourApiClient.findAreaBasedPlaces(anyString(), anyString(), anyString(), anyString(), eq("28"), anyInt(), anyInt()))
                .thenReturn(successResponse());
        when(tourApiClient.findAreaBasedPlaces(anyString(), anyString(), anyString(), anyString(), eq("12"), anyInt(), anyInt()))
                .thenReturn(
                        successResponse(),
                        successResponse(item("300", "12", "Fallback Tourist Spot"))
                );
        when(tourApiClient.findAreaBasedPlaces(anyString(), anyString(), anyString(), anyString(), eq("14"), anyInt(), anyInt()))
                .thenReturn(successResponse());
        when(tourApiClient.findAreaBasedPlaces(anyString(), anyString(), anyString(), anyString(), eq("39"), anyInt(), anyInt()))
                .thenReturn(successResponse());

        List<CandidatePlace> candidates = candidateProvider.findCandidates(command);

        assertThat(candidates)
                .extracting(CandidatePlace::contentId)
                .containsExactly("300");
    }

    private CourseRecommendCommand command() {
        return new CourseRecommendCommand(
                1L,
                "서울",
                "11",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                TravelStyle.NORMAL,
                Transportation.PUBLIC,
                List.of()
        );
    }

    private TourApiResponse successResponse(TourApiResponse.Response.Item... items) {
        return new TourApiResponse(new TourApiResponse.Response(
                new TourApiResponse.Response.Header("0000", "OK"),
                new TourApiResponse.Response.Body(
                        new TourApiResponse.Response.Items(List.of(items)),
                        items.length,
                        1,
                        items.length
                )
        ));
    }

    private TourApiResponse.Response.Item item(String contentId, String contentTypeId, String title) {
        return item(contentId, contentTypeId, title, "");
    }

    private TourApiResponse.Response.Item eventItem(
            String contentId,
            String contentTypeId,
            String title,
            String eventEndDate
    ) {
        return item(contentId, contentTypeId, title, eventEndDate);
    }

    private TourApiResponse.Response.Item item(
            String contentId,
            String contentTypeId,
            String title,
            String eventEndDate
    ) {
        return new TourApiResponse.Response.Item(
                contentId,
                contentTypeId,
                title,
                "서울특별시 중구",
                "",
                "https://example.com/image.jpg",
                "",
                "126.9780",
                "37.5665",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                eventEndDate,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        );
    }
}
