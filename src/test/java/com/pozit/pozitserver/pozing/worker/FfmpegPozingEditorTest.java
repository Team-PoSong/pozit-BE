package com.pozit.pozitserver.pozing.worker;

import org.junit.jupiter.api.Test;

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

}
