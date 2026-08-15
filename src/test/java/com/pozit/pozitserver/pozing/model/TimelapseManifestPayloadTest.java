package com.pozit.pozitserver.pozing.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimelapseManifestPayloadTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesAndDeserializesManifestPayload() throws Exception {
        TimelapseManifestPayload payload = new TimelapseManifestPayload(
                1,
                15L,
                List.of(new TimelapseManifestPayload.CourseManifest(
                        23L,
                        1,
                        List.of(new TimelapseManifestPayload.SpotManifest(
                                101L,
                                55L,
                                "첨성대",
                                1,
                                new BigDecimal("35.8347000"),
                                new BigDecimal("129.2190000"),
                                List.of(
                                        new TimelapseManifestPayload.MemberPozingManifest(1L, "현영", null),
                                        new TimelapseManifestPayload.MemberPozingManifest(2L, "민서", "pozings/123.mp4")
                                )
                        ))
                ))
        );

        String json = objectMapper.writeValueAsString(payload);
        TimelapseManifestPayload deserialized = objectMapper.readValue(json, TimelapseManifestPayload.class);

        assertThat(deserialized).isEqualTo(payload);
    }
}
