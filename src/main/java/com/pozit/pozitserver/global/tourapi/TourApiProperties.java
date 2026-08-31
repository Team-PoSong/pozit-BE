package com.pozit.pozitserver.global.tourapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tour-api")
public record TourApiProperties (
        String baseUrl,
        String serviceKey,
        String mobileOs,
        String mobileApp,
        long timeoutSeconds
){
    public TourApiProperties {
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 20;
        }
    }
}
