package com.pozit.pozitserver.global.config;

import com.pozit.pozitserver.global.openai.OpenAiProperties;
import com.pozit.pozitserver.global.tourapi.TourApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient tourApiWebClient(
            WebClient.Builder webClientBuilder,
            TourApiProperties properties
    ) {
        return webClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public WebClient openAiWebClient(
            WebClient.Builder webClientBuilder,
            OpenAiProperties properties
    ) {
        return webClientBuilder.clone()
                .baseUrl(properties.baseUrl())
                .build();
    }

}
