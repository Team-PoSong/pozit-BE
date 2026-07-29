package com.pozit.pozitserver.global.s3;

import com.pozit.pozitserver.global.config.S3Properties;
import com.pozit.pozitserver.travel.dto.response.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    public PresignedUrlResponse createPutPresignedUrl(
            String key,
            String contentType,
            Duration expiration
    ) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest =
                s3Presigner.presignPutObject(presignRequest);

        return new PresignedUrlResponse(
                presignedRequest.url().toString(),
                createObjectUrl(key)
        );
    }

    private String createObjectUrl(String key) {
        return "https://%s.s3.%s.amazonaws.com/%s"
                .formatted(s3Properties.getBucket(), s3Properties.getRegion(), key);
    }
}
