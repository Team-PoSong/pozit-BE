package com.pozit.pozitserver.pozing.dto.response;

import com.pozit.pozitserver.pozing.domain.PozingEditJobStatus;

import java.time.LocalDateTime;

public record PozingEditJobStatusResponse(
        Long jobId,
        PozingEditJobStatus status,
        String downloadUrl,
        String errorMessage,
        LocalDateTime expiresAt
) {
}
