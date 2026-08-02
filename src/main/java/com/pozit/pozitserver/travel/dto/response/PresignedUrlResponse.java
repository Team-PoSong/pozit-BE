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
                description = "업로드된 파일의 일반 S3 URL입니다. private 객체는 이 URL로 직접 접근할 수 없으며, 서버 저장에는 objectKey를 사용합니다.",
                example = "https://pozit-pozing.s3.ap-northeast-2.amazonaws.com/pozings/1/1/uuid.mp4"
        )
        String fileUrl,

        @Schema(
                description = "S3 object key입니다. 업로드 완료 후 서버에는 URL 전체가 아니라 이 값을 저장합니다.",
                example = "pozings/1/1/uuid.mp4"
        )
        String objectKey
) {}
