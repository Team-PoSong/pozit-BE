package com.pozit.pozitserver.pozing.dto.response;

import com.pozit.pozitserver.pozing.domain.PozingEditJobStatus;

public record PozingEditJobCreateResponse(
        Long jobId,
        PozingEditJobStatus status
) {
}
