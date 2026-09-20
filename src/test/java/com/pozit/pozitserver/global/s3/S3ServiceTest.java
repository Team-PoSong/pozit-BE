package com.pozit.pozitserver.global.s3;

import com.pozit.pozitserver.global.config.S3Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3ServiceTest {

    @Test
    void createPutPresignedUrlDoesNotSignContentType() throws Exception {
        S3Presigner s3Presigner = mock(S3Presigner.class);
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/upload"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        S3Service s3Service = new S3Service(
                mock(S3Client.class),
                s3Presigner,
                s3Properties()
        );

        s3Service.createPutPresignedUrl(
                "pozings/1/2/video.mp4",
                Duration.ofMinutes(10)
        );

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());

        assertThat(captor.getValue().putObjectRequest().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().putObjectRequest().key()).isEqualTo("pozings/1/2/video.mp4");
        assertThat(captor.getValue().putObjectRequest().contentType()).isNull();
    }

    private S3Properties s3Properties() {
        S3Properties properties = new S3Properties();
        properties.getS3().setBucket("test-bucket");
        properties.setRegion(Map.of("static", "ap-northeast-2"));
        return properties;
    }
}
