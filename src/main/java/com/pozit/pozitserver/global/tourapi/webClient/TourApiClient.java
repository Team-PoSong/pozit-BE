package com.pozit.pozitserver.global.tourapi.webClient;

import com.pozit.pozitserver.course.dto.response.coursespot.TourApiResponse;
import com.pozit.pozitserver.global.tourapi.TourApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class TourApiClient {

    private final WebClient tourApiWebClient;
    private final TourApiProperties properties;

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
                .bodyToMono(TourApiResponse.class)
                .block();
    }
}
