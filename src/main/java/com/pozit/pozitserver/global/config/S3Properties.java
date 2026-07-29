package com.pozit.pozitserver.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloud.aws")
public class S3Properties {

    private S3 s3 = new S3();
    private Map<String, String> region = new HashMap<>();
    private Credentials credentials = new Credentials();

    public String getBucket() {
        return s3.getBucket();
    }

    public String getRegion() {
        return region.get("static");
    }

    public String getAccessKey() {
        return credentials.getAccessKey();
    }

    public String getSecretKey() {
        return credentials.getSecretKey();
    }

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
    }

    @Getter
    @Setter
    public static class Credentials {
        private String accessKey;
        private String secretKey;
    }
}
