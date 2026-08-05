package com.pozit.pozitserver.global.tourapi.webClient;

import com.pozit.pozitserver.course.dto.response.coursespot.TourApiResponse;
import com.pozit.pozitserver.global.exception.BusinessException;
import com.pozit.pozitserver.global.exception.ErrorCode;
import com.pozit.pozitserver.global.tourapi.TourApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TourApiClient {

    private final WebClient tourApiWebClient;
    private final TourApiProperties properties;
    private final ObjectMapper objectMapper;

    public TourApiResponse searchPlaces(
            String keyword,
            int page,
            int size
    ){
        return get("/searchKeyword2", page, size, uriBuilder -> uriBuilder
                        .queryParam("keyword",keyword)
        );
    }

    public TourApiResponse findAreaBasedPlaces(
            String legalDongRegionCode,
            String legalDongSigunguCode,
            String areaCode,
            String sigunguCode,
            String contentTypeId,
            int page,
            int size
    ) {
        return get("/areaBasedList2", page, size, uriBuilder -> {
            if (areaCode != null && !areaCode.isBlank()) {
                uriBuilder.queryParam("areaCode", areaCode);
            }
            if (sigunguCode != null && !sigunguCode.isBlank()) {
                uriBuilder.queryParam("sigunguCode", sigunguCode);
            }
            if (legalDongRegionCode != null && !legalDongRegionCode.isBlank()) {
                uriBuilder.queryParam("lDongRegnCd", legalDongRegionCode);
            }
            if (legalDongSigunguCode != null && !legalDongSigunguCode.isBlank()) {
                uriBuilder.queryParam("lDongSignguCd", legalDongSigunguCode);
            }
            if (contentTypeId != null && !contentTypeId.isBlank()) {
                uriBuilder.queryParam("contentTypeId", contentTypeId);
            }
        });
    }

    public TourApiResponse getDetailCommon(String contentId, String contentTypeId) {
        return getDetail("/detailCommon2", contentId, contentTypeId, uriBuilder -> uriBuilder
                .queryParam("defaultYN", "Y")
                .queryParam("firstImageYN", "Y")
                .queryParam("areacodeYN", "Y")
                .queryParam("catcodeYN", "Y")
                .queryParam("addrinfoYN", "Y")
                .queryParam("mapinfoYN", "Y")
                .queryParam("overviewYN", "Y")
        );
    }

    public TourApiResponse getDetailIntro(String contentId, String contentTypeId) {
        return getDetail("/detailIntro2", contentId, contentTypeId, uriBuilder -> {
        });
    }

    public TourApiResponse getDetailInfo(String contentId, String contentTypeId) {
        return getDetail("/detailInfo2", contentId, contentTypeId, uriBuilder -> {
        });
    }

    private TourApiResponse getDetail(
            String path,
            String contentId,
            String contentTypeId,
            java.util.function.Consumer<org.springframework.web.util.UriBuilder> customizer
    ) {
        return get(path, 1, 10, uriBuilder -> {
            uriBuilder.queryParam("contentId", contentId);
            if (contentTypeId != null && !contentTypeId.isBlank()) {
                uriBuilder.queryParam("contentTypeId", contentTypeId);
            }
            customizer.accept(uriBuilder);
        });
    }

    private TourApiResponse get(
            String path,
            int page,
            int size,
            java.util.function.Consumer<org.springframework.web.util.UriBuilder> customizer
    ) {
        return tourApiWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path)
                            .queryParam("serviceKey", properties.serviceKey())
                            .queryParam("MobileOS", properties.mobileOs())
                            .queryParam("MobileApp", properties.mobileApp())
                            .queryParam("_type", "json")
                            .queryParam("pageNo", page)
                            .queryParam("numOfRows", size)
                            .queryParam("arrange", "A");
                    customizer.accept(uriBuilder);
                    return uriBuilder.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, response->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body-> Mono.error(
                                        new BusinessException(ErrorCode.TOUR_API_REQUEST_FAILED)
                                )))
                .bodyToMono(String.class)
                .map(this::deserializeTourApiResponse)
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    private TourApiResponse deserializeTourApiResponse(String body) {
        if (body == null || body.isBlank()) {
            throw new BusinessException(ErrorCode.TOUR_API_REQUEST_FAILED);
        }

        try {
            String normalizedBody = body.replaceAll("\"items\"\\s*:\\s*\"\"", "\"items\":null");
            return objectMapper.readValue(normalizedBody, TourApiResponse.class);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.TOUR_API_REQUEST_FAILED);
        }
    }
}
