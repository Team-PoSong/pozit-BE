package com.pozit.pozitserver.pozing.worker;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FfmpegPozingEditorTest {

    @Test
    void oneMemberUsesEntireBottomArea() {
        List<FfmpegPozingEditor.LayoutCell> cells = FfmpegPozingEditor.calculateVideoCells(1);

        assertThat(cells).containsExactly(new FfmpegPozingEditor.LayoutCell(0, 480, 720, 800));
    }

    @Test
    void twoMembersUseVerticalStackInBottomArea() {
        List<FfmpegPozingEditor.LayoutCell> cells = FfmpegPozingEditor.calculateVideoCells(2);

        assertThat(cells).containsExactly(
                new FfmpegPozingEditor.LayoutCell(0, 480, 720, 400),
                new FfmpegPozingEditor.LayoutCell(0, 880, 720, 400)
        );
    }

    @Test
    void threeMembersUseVerticalStackInBottomArea() {
        List<FfmpegPozingEditor.LayoutCell> cells = FfmpegPozingEditor.calculateVideoCells(3);

        assertThat(cells).containsExactly(
                new FfmpegPozingEditor.LayoutCell(0, 480, 720, 266),
                new FfmpegPozingEditor.LayoutCell(0, 746, 720, 266),
                new FfmpegPozingEditor.LayoutCell(0, 1012, 720, 268)
        );
    }

    @Test
    void fourOrMoreMembersUseTwoColumnGridInBottomArea() {
        List<FfmpegPozingEditor.LayoutCell> cells = FfmpegPozingEditor.calculateVideoCells(4);

        assertThat(cells).containsExactly(
                new FfmpegPozingEditor.LayoutCell(0, 480, 360, 400),
                new FfmpegPozingEditor.LayoutCell(360, 480, 360, 400),
                new FfmpegPozingEditor.LayoutCell(0, 880, 360, 400),
                new FfmpegPozingEditor.LayoutCell(360, 880, 360, 400)
        );
    }

    @Test
    void oddGridMemberCountLeavesRemainingGridSpaceBlank() {
        List<FfmpegPozingEditor.LayoutCell> cells = FfmpegPozingEditor.calculateVideoCells(5);

        assertThat(cells).containsExactly(
                new FfmpegPozingEditor.LayoutCell(0, 480, 360, 266),
                new FfmpegPozingEditor.LayoutCell(360, 480, 360, 266),
                new FfmpegPozingEditor.LayoutCell(0, 746, 360, 266),
                new FfmpegPozingEditor.LayoutCell(360, 746, 360, 266),
                new FfmpegPozingEditor.LayoutCell(0, 1012, 360, 268)
        );
    }

    @Test
    void missingMemberVideoUsesSleepingPozitPlaceholderAndKeepsNickname() {
        FfmpegPozingEditor editor = new FfmpegPozingEditor();

        String filter = editor.buildStackFilter(
                Arrays.asList(Path.of("member-0.mp4"), null),
                List.of(Path.of("nickname-0.png"), Path.of("nickname-1.png")),
                2,
                3.0
        );

        assertThat(filter)
                .contains("color=c=black:s=720x400:r=30:d=3.000,format=yuv420p,setpts=PTS-STARTPTS[empty1];")
                .contains("[1:v]scale=200:200:force_original_aspect_ratio=decrease")
                .contains("[empty1][icon1]overlay=(W-w)/2:(H-h)/2,format=yuv420p[v1base];")
                .contains("[3:v]format=rgba,fps=30,trim=duration=3.000,setpts=PTS-STARTPTS[label1];")
                .contains("[v1base][label1]overlay=40:H-h-40:format=auto,format=yuv420p[v1];");
    }

    @Test
    void segmentWithNoMemberVideosLastsThreeSeconds() {
        FfmpegPozingEditor editor = new FfmpegPozingEditor();

        double duration = editor.calculateSegmentDuration(Arrays.asList(null, null));

        assertThat(duration).isEqualTo(3.0);
    }

}
