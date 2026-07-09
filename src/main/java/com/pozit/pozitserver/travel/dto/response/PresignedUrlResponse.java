package com.pozit.pozitserver.travel.dto.response;

public record PresignedUrlResponse(
        String presignedUrl,
        String imageUrl
) {}
