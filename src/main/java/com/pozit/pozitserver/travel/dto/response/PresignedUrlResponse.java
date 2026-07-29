package com.pozit.pozitserver.travel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "S3 presigned URL 발급 응답")
public record PresignedUrlResponse(
        @Schema(
                description = "클라이언트가 파일을 직접 업로드할 S3 presigned PUT URL입니다. 만료 시간이 있으며 DB에 저장하지 않습니다.",
                example = "https://pozit-pozing.s3.ap-northeast-2.amazonaws.com/pozings/1/1/uuid.mp4?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=..."
        )
        String presignedUrl,

        @Schema(
                description = "업로드 완료 후 접근 가능한 파일 URL입니다. 업로드 성공 후 이 값을 pozingUrl 등으로 서버에 저장합니다.",
                example = "https://pozit-pozing.s3.ap-northeast-2.amazonaws.com/pozings/1/1/uuid.mp4"
        )
        String fileUrl
) {}
