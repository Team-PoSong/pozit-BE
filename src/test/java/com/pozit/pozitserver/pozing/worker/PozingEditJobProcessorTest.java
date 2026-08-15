package com.pozit.pozitserver.pozing.worker;

import com.pozit.pozitserver.pozing.model.TimelapseManifestPayload;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PozingEditJobProcessorTest {

    @Test
    void editSegmentsKeepSpotsWithoutAnyVideos() {
        PozingEditJobProcessor processor = new PozingEditJobProcessor(
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        TimelapseManifestPayload manifest = new TimelapseManifestPayload(
                1,
                10L,
                List.of(new TimelapseManifestPayload.CourseManifest(
                        20L,
                        1,
                        List.of(
                                spot(100L, "첫 번째 장소"),
                                spot(101L, "두 번째 장소")
                        )
                ))
        );

        List<FfmpegPozingEditor.PozingEditSegment> segments = processor.createEditSegments(
                manifest,
                Map.of()
        );

        assertThat(segments).hasSize(2);
        assertThat(segments.get(0).courseSpotId()).isEqualTo(100L);
        assertThat(segments.get(0).currentRouteIndex()).isZero();
        assertThat(segments.get(0).memberVideos()).containsExactly(null, null);
        assertThat(segments.get(1).courseSpotId()).isEqualTo(101L);
        assertThat(segments.get(1).currentRouteIndex()).isEqualTo(1);
        assertThat(segments.get(1).memberVideos()).containsExactly(null, null);
    }

    private TimelapseManifestPayload.SpotManifest spot(Long courseSpotId, String name) {
        return new TimelapseManifestPayload.SpotManifest(
                courseSpotId,
                courseSpotId + 1000,
                name,
                0,
                null,
                null,
                List.of(
                        new TimelapseManifestPayload.MemberPozingManifest(1L, "민서", null),
                        new TimelapseManifestPayload.MemberPozingManifest(2L, "포짓", null)
                )
        );
    }
}
