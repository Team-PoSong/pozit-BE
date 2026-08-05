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
        return tourApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/searchKeyword2")
                        .queryParam("serviceKey",properties.serviceKey())
                        .queryParam("MobileOS",properties.mobileOs())
                        .queryParam("MobileApp",properties.mobileApp())
                        .queryParam("_type","json")
                        .queryParam("keyword",keyword)
                        .queryParam("pageNo",page)
                        .queryParam("numOfRows",size)
                        .queryParam("arrange","A")
                        .build())
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
