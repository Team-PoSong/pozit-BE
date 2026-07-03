package com.pozit.pozitserver.test.dto;

import com.pozit.pozitserver.test.domain.Sample;

public record SampleResponseDto(
        Long id,
        String title,
        String content
) {

    public static SampleResponseDto from(Sample sample) {
        return new SampleResponseDto(
                sample.getId(),
                sample.getTitle(),
                sample.getContent()
        );
    }
}