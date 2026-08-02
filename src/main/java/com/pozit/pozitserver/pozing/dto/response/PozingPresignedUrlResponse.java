package com.pozit.pozitserver.pozing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포징 영상 S3 presigned URL 발급 응답")
public record PozingPresignedUrlResponse(
        @Schema(description = "클라이언트가 파일을 직접 업로드할 S3 presigned PUT URL입니다.")
        String presignedUrl,

        @Schema(description = "업로드 완료 후 /api/pozing/save 호출 시 전달할 업로드 식별자입니다.")
        String uploadId
) {
}
